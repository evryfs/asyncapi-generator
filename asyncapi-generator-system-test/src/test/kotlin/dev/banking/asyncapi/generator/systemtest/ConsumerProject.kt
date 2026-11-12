package dev.banking.asyncapi.generator.systemtest

import java.io.File

/**
 * A dependency of a consumer build; the version comes from the Spring Boot BOM when null.
 *
 * @property group Maven group id.
 * @property artifact Maven artifact id.
 * @property version Explicit version, or null when the BOM manages it.
 */
internal data class Dependency(
    val group: String,
    val artifact: String,
    val version: String? = null,
)

/**
 * Libraries a consumer of generated Kotlin Spring Kafka code needs to compile it and run its checks.
 *
 * @param kotlinVersion Version for Kotlin artifacts the BOM does not manage.
 */
internal class ConsumerDependencies(kotlinVersion: String) {
    val implementation: List<Dependency> = listOf(
        Dependency("org.jetbrains.kotlin", "kotlin-stdlib"),
        Dependency("org.springframework.kafka", "spring-kafka"),
        Dependency("jakarta.validation", "jakarta.validation-api"),
        Dependency("com.fasterxml.jackson.core", "jackson-annotations"),
    )
    val test: List<Dependency> = listOf(
        Dependency("org.jetbrains.kotlin", "kotlin-test-junit5", kotlinVersion),
        Dependency("org.junit.jupiter", "junit-jupiter"),
    )
}

/** Layout shared by Maven and Gradle consumer projects. */
internal object ConsumerProject {
    const val SPEC_DIRECTORY = "src/main/asyncapi"

    /** Creates a clean project directory holding the scenario spec and checks. */
    fun prepare(scenario: Scenario, environment: SystemTestEnvironment, projectDirectory: File): File {
        projectDirectory.deleteRecursively()
        scenario.specDirectory(environment).copyRecursively(projectDirectory.resolve(SPEC_DIRECTORY))
        scenario.checksDirectory(environment)?.copyRecursively(projectDirectory.resolve("src/test/kotlin"))
        return projectDirectory
    }

    fun inputSpecPath(config: GenerationConfig): String = "$SPEC_DIRECTORY/${config.inputSpec}"

    /** Project name derived from the scenario, safe for Maven artifact ids and Gradle project names. */
    fun projectName(scenario: Scenario): String = "system-test-${scenario.name}"
}

internal fun String.xmlEscaped(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

internal fun String.kotlinStringLiteral(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
