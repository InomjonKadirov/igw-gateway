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
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.findVersion("springBoot").get().requiredVersion}")
    }
}

dependencies {
    // Autoconfigure classes: @AutoConfiguration, @ConditionalOnClass, @ConfigurationProperties.
    // Marked implementation (not compileOnly) so the BOM resolves its version.
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Spring Web types (@RestControllerAdvice, ResponseBodyAdvice, WebFilter, MediaType,
    // ServerHttpRequest/Response). The consumer (igw-edge) provides these at runtime via
    // spring-boot-starter-web; the BOM (via spring-framework-bom) resolves the version.
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")

    // Jakarta Servlet API for reading the underlying HttpServletResponse (status code).
    // Spring Framework 7.x removed getStatusCode() from ServerHttpResponse; we read it
    // via the servlet API. Version managed by the Spring Boot BOM.
    implementation("jakarta.servlet:jakarta.servlet-api")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
