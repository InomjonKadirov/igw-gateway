plugins {
    id("spring-boot-library-conventions")
    id("java-conventions")
}

dependencies {
    // jakarta.servlet-api is brought in by the convention plugin.
    // spring-boot-starter-test brings MockHttpServletRequest, MockHttpServletResponse, MockFilterChain.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
