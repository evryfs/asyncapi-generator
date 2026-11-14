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
 * Libraries a consumer build needs to compile the generated code and run the scenario checks.
 *
 * @param versions Project-wide library versions.
 * @param libraries Optional libraries the scenario asks for.
 */
internal class ConsumerDependencies(versions: LibraryVersions, libraries: Set<Library>) {
    val implementation: List<Dependency> = buildList {
        add(Dependency("org.jetbrains.kotlin", "kotlin-stdlib", versions.kotlin))
        add(Dependency("jakarta.validation", "jakarta.validation-api", versions.jakartaValidation))
        add(Dependency("com.fasterxml.jackson.core", "jackson-annotations", versions.jacksonAnnotations))
        if (Library.SPRING_KAFKA in libraries) {
            add(Dependency("org.springframework.kafka", "spring-kafka", versions.springKafka))
        }
        if (Library.AVRO in libraries) {
            add(Dependency("org.apache.avro", "avro", versions.avro))
        }
        if (Library.PROTOBUF in libraries) {
            add(Dependency("com.google.protobuf", "protobuf-java", versions.protobuf))
            add(Dependency("com.google.protobuf", "protobuf-kotlin", versions.protobuf))
        }
    }

    /** Test libraries; Jackson reads bundled documents and schemas in the checks. */
    val test: List<Dependency> = listOf(
        Dependency("org.jetbrains.kotlin", "kotlin-test-junit5", versions.kotlin),
        Dependency("org.junit.jupiter", "junit-jupiter", versions.junit),
        Dependency("com.fasterxml.jackson.core", "jackson-databind", versions.jackson),
        Dependency("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", versions.jackson),
    )

    /** JUnit launcher Gradle needs at test runtime; JUnit 6 versions it together with Jupiter. */
    val gradleTestRuntime: List<Dependency> = listOf(
        Dependency("org.junit.platform", "junit-platform-launcher", versions.junit),
    )
}

/** Layout shared by the CLI working directory and the Maven and Gradle consumer projects. */
internal object ConsumerProject {
    const val SPEC_DIRECTORY = "src/main/asyncapi"

    /**
     * Creates a clean project directory holding the scenario spec, hand-written
     * sources, checks, and the shared check support code.
     */
    fun prepare(scenario: Scenario, environment: SystemTestEnvironment, projectDirectory: File): File {
        projectDirectory.deleteRecursively()
        projectDirectory.mkdirs()
        scenario.specDirectory(environment)?.copyRecursively(projectDirectory.resolve(SPEC_DIRECTORY))
        scenario.mainDirectory(environment)?.copyRecursively(projectDirectory.resolve("src/main"))
        scenario.checksDirectory(environment)?.let { checks ->
            checks.copyRecursively(projectDirectory.resolve("src/test/kotlin"))
            environment.checkSupport.copyRecursively(projectDirectory.resolve("src/test/kotlin"))
        }
        return projectDirectory
    }

    fun inputSpecPath(execution: Execution): String = "$SPEC_DIRECTORY/${execution.inputSpec}"

    /** Project name derived from the scenario, safe for Maven artifact ids and Gradle project names. */
    fun projectName(scenario: Scenario): String = "system-test-${scenario.name}"
}

/**
 * What one frontend did for a scenario.
 *
 * @property result The (last) process run; the failing one when a run failed.
 * @property outputDirectories Resolved output directory of every execution, by execution id.
 * @property outputFiles Resolved bundled document of every execution that writes one, by execution id.
 */
internal data class FrontendRun(
    val result: ProcessResult,
    val outputDirectories: Map<String, File>,
    val outputFiles: Map<String, File>,
) {
    companion object {
        /** Resolves each execution's paths against [projectDirectory], using [defaultOutputDirectory] when unset. */
        fun of(
            scenario: Scenario,
            projectDirectory: File,
            result: ProcessResult,
            defaultOutputDirectory: (Execution) -> String,
        ): FrontendRun = FrontendRun(
            result = result,
            outputDirectories = scenario.executions.associate { execution ->
                execution.id to projectDirectory.resolve(execution.outputDirectory ?: defaultOutputDirectory(execution))
            },
            outputFiles = scenario.executions.mapNotNull { execution ->
                execution.outputFile?.let { execution.id to projectDirectory.resolve(it) }
            }.toMap(),
        )
    }
}

internal fun String.xmlEscaped(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

internal fun String.kotlinStringLiteral(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
