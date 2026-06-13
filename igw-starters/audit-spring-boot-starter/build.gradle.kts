import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("spring-boot-library-conventions")
    id("java-conventions")
    id("test-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    implementation(libs.findLibrary("spring-boot-starter-data-jdbc").get())

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
