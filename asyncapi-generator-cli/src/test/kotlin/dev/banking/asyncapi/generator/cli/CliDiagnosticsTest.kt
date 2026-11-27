package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises user-facing diagnostics through Clikt's command test boundary.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorCli`
 */
class CliDiagnosticsTest {
    @Test
    fun `should report malformed input without an exception trace`(
        @TempDir tempDir: Path,
    ) {
        val inputFile = File("src/test/resources/diagnostics/asyncapi-malformed.yaml")
        val outputFile = tempDir.resolve("bundled.yaml").toFile()

        val result =
            AsyncApiGeneratorCli().test(
                "--input-spec",
                inputFile.absolutePath,
                "--generator-name",
                "asyncapi-yaml",
                "--output-file",
                outputFile.absolutePath,
            )

        assertEquals(1, result.statusCode)
        assertTrue(
            result.stderr.contains(
                "Malformed input document at line 2, column 1: ${inputFile.absolutePath}",
            ),
            result.stderr,
        )
        assertFalse(result.stderr.contains("DocumentReadException"))
        assertFalse(result.stderr.contains("\tat "))
        assertFalse(outputFile.exists())
    }
}
