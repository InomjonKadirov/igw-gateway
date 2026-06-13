import io.spring.gradle.dependencymanagement.dsl.DependenciesHandler
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("java-conventions")
    id("test-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.findVersion("springBoot").get().requiredVersion}")
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // WireMock for the embedded upstream mock.
    // The version (3.10.0) matches what test-conventions already pulls in
    // transitively, so we don't double-up on different wiremock-jetty12 versions.
    testImplementation(libs.findLibrary("wiremock-spring-boot").get())

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// Pin Jetty to 12.0.16 to match what wiremock-jetty12 3.13.0 ships. Without
// this override, the Spring Boot dependency-management BOM upgrades
// jetty-server / jetty-util to 12.1.10 while the bundled jetty-ee10-servlet
// stays at 12.0.16 — and the older ServletContextHandler calls
// Environment#ensure, which 12.1.x jetty-util has removed, breaking WireMock
// startup with NoSuchMethodError. `resolutionStrategy.force` is silently
// overridden by the BOM import's (c)-marked constraints, so we re-declare the
// versions via the Spring dependency-management DSL which takes precedence.
val jettyPin = "12.0.16"
dependencyManagement {
    val jettyCoords = listOf(
        "org.eclipse.jetty:jetty-bom",
        "org.eclipse.jetty:jetty-server",
        "org.eclipse.jetty:jetty-util",
        "org.eclipse.jetty:jetty-io",
        "org.eclipse.jetty:jetty-http",
        "org.eclipse.jetty:jetty-security",
        "org.eclipse.jetty:jetty-session",
        "org.eclipse.jetty:jetty-xml",
        "org.eclipse.jetty:jetty-client",
        "org.eclipse.jetty:jetty-proxy",
        "org.eclipse.jetty:jetty-alpn-client",
        "org.eclipse.jetty:jetty-alpn-java-client",
        "org.eclipse.jetty:jetty-alpn-server",
        "org.eclipse.jetty:jetty-alpn-java-server",
        "org.eclipse.jetty.ee10:jetty-ee10-bom",
        "org.eclipse.jetty.ee10:jetty-ee10-servlet",
        "org.eclipse.jetty.ee10:jetty-ee10-servlets",
        "org.eclipse.jetty.ee10:jetty-ee10-webapp",
        "org.eclipse.jetty.http2:jetty-http2-common",
        "org.eclipse.jetty.http2:jetty-http2-server",
        "org.eclipse.jetty.http2:jetty-http2-hpack",
    )
    dependencies(object : Action<DependenciesHandler> {
        override fun execute(deps: DependenciesHandler) {
            jettyCoords.forEach { coords ->
                deps.dependency("$coords:$jettyPin")
            }
        }
    })
}
