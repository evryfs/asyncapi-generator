package dev.banking.asyncapi.generator.systemtest

import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Outcome of an external process.
 *
 * @property exitCode Process exit code.
 * @property log File holding the combined stdout and stderr of the process.
 */
internal data class ProcessResult(
    val exitCode: Int,
    val log: File,
) {
    val output: String
        get() = log.readText()
}

/**
 * Runs CLI, Maven, and Gradle as real external processes.
 *
 * Output goes to a log file instead of memory so a failing build can be
 * inspected afterwards; assertion messages include the tail of that log.
 *
 * @param timeout Upper bound for a single process; the process is destroyed when exceeded.
 */
internal class ProcessRunner(private val timeout: Duration) {

    fun run(
        command: List<String>,
        workingDirectory: File,
        log: File,
        environment: Map<String, String> = emptyMap(),
    ): ProcessResult {
        log.parentFile.mkdirs()
        val process = ProcessBuilder(command)
            .directory(workingDirectory)
            .redirectErrorStream(true)
            .redirectOutput(log)
            .apply { environment().putAll(environment) }
            .start()

        if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
            process.descendants().forEach(ProcessHandle::destroyForcibly)
            process.destroyForcibly()
            throw AssertionError("Timed out after $timeout: ${command.joinToString(" ")}\n${tail(log)}")
        }
        return ProcessResult(process.exitValue(), log)
    }

    fun runSuccessfully(
        command: List<String>,
        workingDirectory: File,
        log: File,
        environment: Map<String, String> = emptyMap(),
    ): ProcessResult {
        val result = run(command, workingDirectory, log, environment)
        if (result.exitCode != 0) {
            throw AssertionError(
                "Exit code ${result.exitCode}: ${command.joinToString(" ")}\nFull log: $log\n${tail(log)}",
            )
        }
        return result
    }

    private fun tail(log: File, lines: Int = 60): String =
        log.takeIf(File::isFile)?.readLines()?.takeLast(lines)?.joinToString("\n").orEmpty()
}
