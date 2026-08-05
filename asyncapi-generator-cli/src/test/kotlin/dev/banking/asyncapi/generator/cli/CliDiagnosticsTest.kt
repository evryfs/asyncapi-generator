package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.testing.test
import dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader
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
    fun `should report malformed input without an exception trace`(@TempDir tempDir: Path) {
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

    @Test
    fun `should preserve formatted validation errors`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/diagnostics/asyncapi-validation-error.yaml")
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
        assertTrue(result.stderr.contains("Validation failed with 1 error(s):"))
        assertTrue(result.stderr.contains("Info 'title' field is required and cannot be empty."))
        assertTrue(result.stderr.contains("asyncapi-validation-error.yaml"))
        assertTrue(result.stderr.contains("title: ''"))
        assertFalse(result.stderr.contains("AsyncApiValidateException"))
        assertFalse(outputFile.exists())
    }

    @Test
    fun `should accept an arbitrary application version and complete generation`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/diagnostics/asyncapi-validation-warning.yaml")
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

        assertEquals(0, result.statusCode)
        assertTrue(result.stderr.isBlank(), result.stderr)
        assertTrue(result.stdout.contains("Generation complete."))
        assertTrue(outputFile.exists())
        val generatedDocument = AsyncApiDocumentLoader().load(outputFile).document
        assertEquals("release-candidate", generatedDocument.info.version)
    }
}
