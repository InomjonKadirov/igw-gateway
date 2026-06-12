import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("spring-boot-app-conventions")
    id("test-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    // Starters are added in PRs #2-#7.
    // audit-spring-boot-starter is added in PR #7 (it brings spring-boot-starter-data-jdbc
    // which requires a real or in-memory DataSource — not present in PR #1).
    implementation(project(":igw-starters:envelope-spring-boot-starter"))
    implementation(project(":igw-starters:error-mapping-spring-boot-starter"))
    implementation(project(":igw-starters:resilience-spring-boot-starter"))
    implementation(project(":igw-starters:token-cache-spring-boot-starter"))
    implementation(project(":igw-starters:i18n-spring-boot-starter"))

    // TODO(PR #8): add Spring Cloud Gateway starter + Spring Security / OAuth2 resource server
    // for JWT validation. PR #1 ships only the placeholder application class.
    implementation(libs.findLibrary("nimbus-jose-jwt").get())
    implementation(libs.findLibrary("bucket4j-core").get())

    testImplementation(libs.findLibrary("spring-modulith-testing").get())
}
