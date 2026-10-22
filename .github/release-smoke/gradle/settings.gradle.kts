pluginManagement {
    plugins {
        id("dev.banking.asyncapi.generator") version providers.gradleProperty("generatorVersion").get()
    }
    repositories {
        when (System.getenv("SMOKE_DESTINATION")) {
            "gradle-central" -> mavenCentral()
            "gradle-portal" -> {
                exclusiveContent {
                    forRepository { gradlePluginPortal() }
                    filter {
                        includeModule("dev.banking.asyncapi.generator", "dev.banking.asyncapi.generator.gradle.plugin")
                        includeModule("dev.banking.asyncapi.generator", "asyncapi-generator-gradle-plugin")
                    }
                }
                mavenCentral()
            }
            else -> error("Expected a Gradle publication destination.")
        }
    }
}

rootProject.name = "published-generator-consumer"
