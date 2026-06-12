plugins {
    id("spring-boot-library-conventions")
    id("test-conventions")
}

dependencies {
    implementation(libs.spring.boot.starter.data.redis)
}
