plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Plugin classpath for convention plugins to apply without explicit versions.
    implementation(libs.dependency.management.plugin)
    implementation(libs.spring.boot.gradle.plugin)
}
