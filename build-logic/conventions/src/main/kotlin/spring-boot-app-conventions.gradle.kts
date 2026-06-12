import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    java
    id("io.spring.dependency-management")
    id("org.springframework.boot")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.findVersion("springBoot").get().requiredVersion}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.findVersion("springCloud").get().requiredVersion}")
    }
}

dependencies {
    // The bare starter provides @SpringBootApplication, SpringApplication, autoconfigure,
    // and logging. Apps add their own web/security/etc. starters on top.
    implementation(libs.findLibrary("spring-boot-starter").get())
}

tasks.withType<Test> {
    useJUnitPlatform()
}
