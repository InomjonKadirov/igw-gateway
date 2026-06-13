import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("spring-boot-library-conventions")
    id("java-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    // resilience4j-spring-boot3 provides CircuitBreakerRegistry, BulkheadRegistry,
    // TimeLimiterRegistry, RetryRegistry, and the @CircuitBreaker / @Bulkhead AOP
    // integration. Per-provider instances are configured by the consuming service
    // in application.yaml under resilience4j.circuitbreaker.instances.<name>.* and
    // resilience4j.bulkhead.instances.<name>.* — this starter does not register them.
    implementation(libs.findLibrary("resilience4j-spring-boot3").get())
    implementation(libs.findLibrary("apache-httpclient5").get())

    // spring-boot-starter-test (BOM-managed): JUnit 5, AssertJ, Mockito, Hamcrest,
    // spring-test, MockMvc, MockHttpServletRequest/Response, etc. Not in the catalog
    // because the version comes from the Spring Boot BOM (no explicit version needed).
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
