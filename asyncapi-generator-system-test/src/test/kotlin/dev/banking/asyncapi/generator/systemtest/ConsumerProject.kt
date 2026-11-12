package dev.banking.asyncapi.generator.systemtest

import java.io.File

/**
 * A dependency of a consumer build.
 *
 * @property group Maven group id.
 * @property artifact Maven artifact id.
 * @property version Version, taken from the project's shared [LibraryVersions].
 */
internal data class Dependency(
    val group: String,
    val artifact: String,
    val version: String,
) {
    override fun toString(): String = "$group:$artifact:$version"
}

/**
 * Libraries a consumer of generated Kotlin Spring Kafka code needs to compile it and run its checks.
 *
 * @param versions Project-wide library versions.
 */
internal class ConsumerDependencies(versions: LibraryVersions) {
    val implementation: List<Dependency> = listOf(
        Dependency("org.jetbrains.kotlin", "kotlin-stdlib", versions.kotlin),
        Dependency("org.springframework.kafka", "spring-kafka", versions.springKafka),
        Dependency("jakarta.validation", "jakarta.validation-api", versions.jakartaValidation),
        Dependency("com.fasterxml.jackson.core", "jackson-annotations", versions.jacksonAnnotations),
    )
    val test: List<Dependency> = listOf(
        Dependency("org.jetbrains.kotlin", "kotlin-test-junit5", versions.kotlin),
        Dependency("org.junit.jupiter", "junit-jupiter", versions.junit),
    )

    /** JUnit launcher Gradle needs at test runtime; JUnit 6 versions it together with Jupiter. */
    val gradleTestRuntime: List<Dependency> = listOf(
        Dependency("org.junit.platform", "junit-platform-launcher", versions.junit),
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
