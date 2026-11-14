package dev.banking.asyncapi.generator.systemtest

import java.io.File

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

    /** Copies the scenario's build file at [path] into [projectDirectory] under its own name. */
    fun copyBuildFile(scenario: Scenario, environment: SystemTestEnvironment, path: String, projectDirectory: File) {
        val source = scenario.directory(environment).resolve(path)
        check(source.isFile) { "Scenario ${scenario.name} has no build file $source" }
        source.copyTo(projectDirectory.resolve(source.name))
    }
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
