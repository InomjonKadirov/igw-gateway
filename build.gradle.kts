import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.TimeUnit

subprojects {
    group = "uz.thinkhub.igw"
    version = "0.0.1-SNAPSHOT"
}

// ---------------------------------------------------------------------------
// k6 smoke: stress-probe igw-edge against local/echo-server.
//
// This is a saturation probe, not a regression gate — see local/k6/README.md
// for what it does and does not prove. Byte-parity is the real acceptance
// signal and lives in local/k6/byte-diff.sh.
// ---------------------------------------------------------------------------

val k6Smoke by tasks.registering {
    group = "verification"
    description = "Run the k6 saturation probe against igw-edge + local/echo-server and assert byte parity."

    // The k6Smoke task reads runtimeClasspath from the sourceSets, which
    // points at build/resources/main (the output of processResources).
    // Without this dependsOn, a fresh edit to application.yaml is not
    // picked up and the smoke runs against a stale config.
    dependsOn(":igw-edge:processResources", ":local:echo-server:processResources")

    val skipK6 = providers.gradleProperty("k6.skip").map(String::toBoolean).orElse(false)
    val k6Script = file("local/k6/smoke.js")
    val byteDiff = file("local/k6/byte-diff.sh")
    val k6ReportDir = file("local/k6/reports")
    val k6LogDir = layout.buildDirectory.dir("k6-logs")

    doLast {
        require(k6Script.exists()) { "k6 script not found: ${k6Script.absolutePath}" }
        require(byteDiff.exists()) { "byte-diff script not found: ${byteDiff.absolutePath}" }

        k6ReportDir.mkdirs()
        val k6Logs = k6LogDir.get().asFile.also { it.mkdirs() }
        k6Logs.resolve("echo-server.log").delete()
        k6Logs.resolve("igw-edge.log").delete()

        val running = mutableListOf<Pair<String, Process>>()

        fun startJava(
            label: String,
            mainClass: String,
            classpath: org.gradle.api.file.FileCollection,
            jvmArgs: List<String>,
            workingDir: File,
            logFile: File,
        ) {
            val javaBin = File(System.getProperty("java.home"), "bin/java")
            val command = listOf(javaBin.absolutePath) +
                    jvmArgs +
                    listOf("-cp", classpath.asPath, mainClass)

            logFile.parentFile.mkdirs()
            val process = ProcessBuilder(command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                .start()
            running.add(label to process)
            logger.lifecycle("k6Smoke: started $label pid=${process.pid()} log=$logFile")
        }

        fun waitForHttp(url: String, label: String, timeoutMs: Long = 60_000) {
            val deadline = System.currentTimeMillis() + timeoutMs
            var lastError: Throwable? = null
            while (System.currentTimeMillis() < deadline) {
                try {
                    val conn = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
                        connectTimeout = 1_000
                        readTimeout = 1_000
                        requestMethod = "GET"
                        instanceFollowRedirects = false
                    }
                    val code = conn.responseCode
                    // 200 (echo) or 4xx (igw-edge with no JWT) — both mean
                    // the server is up and the route is wired.
                    if (code in 200..499) {
                        logger.lifecycle("k6Smoke: $label is up ($url -> $code)")
                        return
                    }
                } catch (t: Throwable) {
                    lastError = t
                }
                Thread.sleep(500)
            }
            throw GradleException(
                "k6Smoke: $label did not become ready at $url within ${timeoutMs}ms (lastError=$lastError)"
            )
        }

        fun stopAll() {
            running.forEach { (label, p) ->
                try {
                    if (p.isAlive) {
                        logger.lifecycle("k6Smoke: stopping $label pid=${p.pid()}")
                        p.destroy()
                        if (!p.waitFor(5, TimeUnit.SECONDS)) {
                            p.destroyForcibly()
                        }
                    }
                } catch (t: Throwable) {
                    logger.warn("k6Smoke: failed to stop $label: ${t.message}")
                }
            }
            running.clear()
        }

        try {
            // Spring 'local' profile on both services. For now this is a
            // no-op (no application-local.yml exists yet); we pass it so
            // future filter-disable config has a single place to live.
            val springProfiles = "local"

            val edgeProject = rootProject.project(":igw-edge")
            val echoProject = rootProject.project(":local:echo-server")

            val javaPluginEdge = edgeProject.extensions.getByType(JavaPluginExtension::class.java)
            val javaPluginEcho = echoProject.extensions.getByType(JavaPluginExtension::class.java)

            // 1. Start echo-server on 8081.
            startJava(
                label = "echo-server",
                mainClass = "uz.thinkhub.igw.echo.EchoServerApplication",
                classpath = javaPluginEcho.sourceSets.named("main").get().runtimeClasspath,
                jvmArgs = listOf(
                    "-Dserver.port=8081",
                    "-Dspring.profiles.active=$springProfiles",
                ),
                workingDir = echoProject.projectDir,
                logFile = File(k6Logs, "echo-server.log"),
            )

            // 2. Start igw-edge on 8080. Mirrors the `bootRun` task's
            // classpath and main class so we exercise the same code path
            // developers hit via `./gradlew :igw-edge:bootRun`. The Spring
            // Boot plugin's @SpringBootApplication auto-discovery resolves
            // the main class without an explicit mainClass declaration in
            // build.gradle.kts, so we walk the classpath looking for the
            // single @SpringBootApplication class. For Phase 0 there is
            // exactly one per service; revisit when that stops being true.
            val edgeMain = "uz.thinkhub.igw.gateway.IgwEdgeApplication"
            startJava(
                label = "igw-edge",
                mainClass = edgeMain,
                classpath = javaPluginEdge.sourceSets.named("main").get().runtimeClasspath,
                jvmArgs = listOf(
                    "-Dserver.port=8080",
                    "-Dspring.profiles.active=$springProfiles",
                    // The Spring Boot OAuth2 resource-server autoconfig
                    // builds a JwtDecoder eagerly when jwk-set-uri is set.
                    // On the local profile the wide-open chain is in effect
                    // (see SecurityConfig#localPermissiveFilterChain), so
                    // JwtDecoder is never actually invoked — but the bean
                    // must still construct. Supply a placeholder URI.
                    "-Dspring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/realms/igw/protocol/openid-connect/certs",
                ),
                workingDir = edgeProject.projectDir,
                logFile = File(k6Logs, "igw-edge.log"),
            )

            // 3. Wait for both.
            waitForHttp("http://localhost:8081/echo", "echo-server")
            waitForHttp("http://localhost:8080/echo", "igw-edge")

            // 4. Run k6 (unless skipped).
            if (skipK6.get()) {
                logger.lifecycle("k6Smoke: k6.skip=true, skipping load step")
            } else {
                val k6Bin = System.getenv("K6_BIN")
                    ?: System.getenv("PATH")
                        ?.split(File.pathSeparator)
                        ?.map { File(it, "k6") }
                        ?.firstOrNull { it.exists() && it.canExecute() }
                        ?.absolutePath
                require(!k6Bin.isNullOrBlank()) {
                    "k6Smoke: k6 binary not found on PATH. Install with 'brew install k6' " +
                            "(macOS), 'apt install k6' (Debian/Ubuntu), or set K6_BIN. " +
                            "Set -Pk6.skip=true to skip the load step but still run byte-diff."
                }
                logger.lifecycle("k6Smoke: running $k6Bin run ${k6Script.absolutePath}")
                val k6Exit = serviceOf<ExecOperations>().exec {
                    commandLine(k6Bin, "run", k6Script.absolutePath)
                    isIgnoreExitValue = true
                }
                if (k6Exit.exitValue != 0) {
                    throw GradleException(
                        "k6Smoke: k6 exited with ${k6Exit.exitValue}. " +
                                "See ${k6ReportDir.absolutePath}/summary.json"
                    )
                }
            }

            // 5. Run byte-diff. The script fails non-zero on any payload diff.
            logger.lifecycle("k6Smoke: running byte-diff ${byteDiff.absolutePath}")
            val diffExit = serviceOf<ExecOperations>().exec {
                commandLine(byteDiff.absolutePath)
                isIgnoreExitValue = true
            }
            if (diffExit.exitValue != 0) {
                throw GradleException(
                    "k6Smoke: byte-diff failed (exit=${diffExit.exitValue}). " +
                            "See ${k6ReportDir.absolutePath}/baseline.bin and proxied.bin"
                )
            }
        } finally {
            stopAll()
        }
    }
}
