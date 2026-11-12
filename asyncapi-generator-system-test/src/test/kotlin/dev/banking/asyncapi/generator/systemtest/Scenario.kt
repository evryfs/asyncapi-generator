package dev.banking.asyncapi.generator.systemtest

import java.io.File

/**
 * A contract plus a generator configuration, exercised end to end by every frontend.
 *
 * On disk a scenario is `src/test/scenarios/<name>/` with a `spec/` directory
 * holding the AsyncAPI document and everything it references, and an optional
 * `checks/` directory holding Kotlin tests. The checks are copied into each
 * consumer build, compiled against the generated code, and run there.
 *
 * @property name Directory name under the scenarios root.
 * @property config Generator configuration rendered for the CLI, Maven, and Gradle.
 */
internal data class Scenario(
    val name: String,
    val config: GenerationConfig,
) {
    fun directory(environment: SystemTestEnvironment): File =
        environment.scenarios.resolve(name).also {
            require(it.isDirectory) { "Scenario directory not found: $it" }
        }

    fun specDirectory(environment: SystemTestEnvironment): File = directory(environment).resolve("spec")

    fun checksDirectory(environment: SystemTestEnvironment): File? =
        directory(environment).resolve("checks").takeIf(File::isDirectory)
}
