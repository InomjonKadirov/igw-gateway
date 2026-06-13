import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("spring-boot-library-conventions")
    id("java-conventions")
    id("test-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    // spring-boot-starter-data-redis brings StringRedisTemplate, Lettuce client,
    // and the RedisConnectionFactory auto-configuration.
    implementation(libs.findLibrary("spring-boot-starter-data-redis").get())

    // spring-boot-starter-test (BOM-managed): JUnit 5, AssertJ, Mockito, Hamcrest,
    // spring-test, MockMvc, MockHttpServletRequest/Response, etc.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
