pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "igw-gateway"

include(
    "igw-edge",
    "igw-starters:envelope-spring-boot-starter",
    "igw-starters:audit-spring-boot-starter",
    "igw-starters:error-mapping-spring-boot-starter",
    "igw-starters:resilience-spring-boot-starter",
    "igw-starters:token-cache-spring-boot-starter",
    "igw-starters:i18n-spring-boot-starter",
    "golden-tests",
    "local:echo-server",
)
