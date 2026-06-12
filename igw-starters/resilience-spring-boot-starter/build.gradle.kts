plugins {
    id("spring-boot-library-conventions")
}

dependencies {
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.apache.httpclient5)
}
