plugins {
    id("spring-boot-library-conventions")
    id("java-conventions")
}

dependencies {
    // spring-boot-starter-test brings JUnit Jupiter, AssertJ, Mockito, Hamcrest,
    // Spring Test, MockMvc, JsonPath, and WebApplicationContextRunner.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
