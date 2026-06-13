import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("spring-boot-app-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    // Web stack so the @RestController echo route compiles. The convention
    // plugin declares these in its body, but the catalog access from
    // precompiled script plugins doesn't propagate reliably, so we
    // repeat the dep here.
    implementation(libs.findLibrary("spring-boot-starter-web").get())

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
