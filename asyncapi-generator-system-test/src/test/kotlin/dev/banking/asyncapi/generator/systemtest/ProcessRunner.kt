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

    /** The last [lines] lines of the log, for assertion messages. */
    fun tail(lines: Int = 60): String =
        log.takeIf(File::isFile)?.readLines()?.takeLast(lines)?.joinToString("\n").orEmpty()

    /** Fails with the log tail unless the process exited with code 0. */
    fun assertSuccess(description: String) {
        if (exitCode != 0) {
            throw AssertionError("$description failed with exit code $exitCode. Full log: $log\n${tail()}")
        }
    }
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
            throw AssertionError("Timed out after $timeout: ${command.joinToString(" ")}\nFull log: $log")
        }
        return ProcessResult(process.exitValue(), log)
    }
}
