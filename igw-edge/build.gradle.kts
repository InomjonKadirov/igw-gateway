import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("spring-boot-app-conventions")
    id("test-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    // Starters
    implementation(project(":igw-starters:envelope-spring-boot-starter"))
    implementation(project(":igw-starters:error-mapping-spring-boot-starter"))
    implementation(project(":igw-starters:resilience-spring-boot-starter"))
    implementation(project(":igw-starters:token-cache-spring-boot-starter"))
    implementation(project(":igw-starters:i18n-spring-boot-starter"))
    // audit-spring-boot-starter is added when the gateway has a real DataSource
    // (the local docker-compose in PR #12).

    // Spring Cloud Gateway MVC (server-webmvc flavor; no WebFlux). BOM-managed
    // by the spring-cloud-dependencies BOM imported in spring-boot-app-conventions.
    implementation(libs.findLibrary("spring-cloud-starter-gateway-server-webmvc").get())

    // JWT validation: spring-boot-starter-oauth2-resource-server pulls in
    // spring-security-oauth2-resource-server, which provides JwtDecoder.
    implementation(libs.findLibrary("spring-boot-starter-security").get())
    implementation(libs.findLibrary("spring-boot-starter-oauth2-resource-server").get())
    implementation(libs.findLibrary("nimbus-jose-jwt").get())
    implementation(libs.findLibrary("bucket4j-core").get())

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
