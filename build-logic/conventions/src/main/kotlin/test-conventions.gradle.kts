import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    java
    id("io.spring.dependency-management")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${libs.findVersion("testcontainers").get().requiredVersion}")
    }
}

dependencies {
    testImplementation(libs.findLibrary("testcontainers-junit-jupiter").get())
    testImplementation(libs.findLibrary("testcontainers-postgresql").get())
    testImplementation(libs.findLibrary("testcontainers-toxiproxy").get())
    testImplementation(libs.findLibrary("wiremock-spring-boot").get())
}

tasks.withType<Test> {
    useJUnitPlatform()
}
