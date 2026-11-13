package dev.banking.asyncapi.generator.systemtest

import java.io.File

/**
 * Contracts plus generator executions, exercised end to end by every frontend.
 *
 * On disk a scenario is `src/test/scenarios/<name>/` with these optional parts:
 * - `spec/`: AsyncAPI documents and everything they reference.
 * - `main/`: hand-written consumer sources, copied to the consumer's `src/main/`.
 * - `checks/`: Kotlin tests, copied to the consumer's `src/test/kotlin/`,
 *   compiled against the generated code, and run there.
 *
 * @property name Directory name under the scenarios root.
 * @property executions Generator executions, run in order.
 * @property client Client configuration for the executions that set a client package.
 * @property libraries Libraries the consumer builds add on top of the base set.
 * @property expectedFailure When set, every frontend must fail with this diagnostic.
 * @property knownIssues Tests of this scenario that fail because of a known bug, by test
 *   display name, with the bug as value. Such a test must keep failing; once the bug is
 *   fixed it fails until its entry is removed.
 */
internal data class Scenario(
    val name: String,
    val executions: List<Execution>,
    val client: ClientConfig? = null,
    val libraries: Set<Library> = emptySet(),
    val expectedFailure: ExpectedFailure? = null,
    val knownIssues: Map<String, String> = emptyMap(),
) {
    init {
        require(executions.isNotEmpty()) { "Scenario $name has no executions" }
        require(executions.map(Execution::id).toSet().size == executions.size) {
            "Scenario $name has duplicate execution ids"
        }
    }

    fun directory(environment: SystemTestEnvironment): File = environment.scenarios.resolve(name)

    fun specDirectory(environment: SystemTestEnvironment): File? = part(environment, "spec")

    fun mainDirectory(environment: SystemTestEnvironment): File? = part(environment, "main")

    fun checksDirectory(environment: SystemTestEnvironment): File? = part(environment, "checks")

    private fun part(environment: SystemTestEnvironment, name: String): File? =
        directory(environment).resolve(name).takeIf(File::isDirectory)
}

/**
 * Optional libraries a scenario's consumer builds need for its generated code or checks.
 */
internal enum class Library {
    /** Spring Kafka, for generated client contracts and `@Validated`. */
    SPRING_KAFKA,

    /** Apache Avro, for generated SpecificRecords and parsing `.avsc` schemas. */
    AVRO,

    /** Protobuf Java and Kotlin runtimes, for generated messages and Kotlin DSLs. */
    PROTOBUF,
}

/**
 * A failure every frontend must report.
 *
 * @property message Text the build output must contain.
 * @property absent Text the build output must not contain, such as a diagnostic that should not be reached.
 */
internal data class ExpectedFailure(
    val message: String,
    val absent: List<String> = emptyList(),
)
