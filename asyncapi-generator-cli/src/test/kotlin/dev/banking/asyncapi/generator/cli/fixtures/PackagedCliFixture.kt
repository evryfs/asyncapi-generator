package dev.banking.asyncapi.generator.cli.fixtures

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Launches the packaged CLI through its executable JAR boundary.
 *
 * Expected behavior is covered by:
 * - `CliPackagedApplicationIT`
 */
internal object PackagedCliFixture {

    fun run(vararg arguments: String): PackagedCliResult {
        val executableJar =
            File(
                requireNotNull(System.getProperty(CLI_JAR_PROPERTY)) {
                    "Missing packaged CLI system property: $CLI_JAR_PROPERTY"
                },
            )
        require(executableJar.isFile) {
            "Packaged CLI does not exist: ${executableJar.absolutePath}"
        }

        val process =
            ProcessBuilder(
                javaExecutable().absolutePath,
                "-jar",
                executableJar.absolutePath,
                *arguments,
            ).redirectErrorStream(true)
                .start()

        if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("Packaged CLI did not finish within $PROCESS_TIMEOUT_SECONDS seconds")
        }

        return PackagedCliResult(
            exitCode = process.exitValue(),
            output = process.inputStream.bufferedReader().use { reader -> reader.readText() },
        )
    }

    private fun javaExecutable(): File =
        File(
            System.getProperty("java.home"),
            "bin/java",
        )

    private const val CLI_JAR_PROPERTY = "asyncapi.generator.cli.jar"
    private const val PROCESS_TIMEOUT_SECONDS = 60L
}

/**
 * Process result returned by [PackagedCliFixture].
 */
internal data class PackagedCliResult(
    val exitCode: Int,
    val output: String,
)
