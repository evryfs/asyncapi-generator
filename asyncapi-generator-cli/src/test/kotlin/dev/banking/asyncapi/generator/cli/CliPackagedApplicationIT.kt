package dev.banking.asyncapi.generator.cli

import dev.banking.asyncapi.generator.cli.fixtures.PackagedCliFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies the CLI as the executable shaded JAR distributed to users.
 *
 * All scenarios use [PackagedCliFixture] so they cross the same process and
 * executable-JAR boundary as an installed CLI invocation.
 */
class CliPackagedApplicationIT {

    @Test
    fun `should expose command help from the packaged CLI`() {
        val result = PackagedCliFixture.run("--help")

        assertEquals(0, result.exitCode)
        assertTrue(result.output.contains("Usage: asyncapi-generator"))
        assertTrue(result.output.contains("--input-spec"))
        assertTrue(result.output.contains("--generator-name"))
    }

    @Test
    fun `should generate a bundled document through the packaged CLI`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_spring_kafka.yaml")
        val outputFile = tempDir.resolve("bundled.yaml").toFile()

        val result =
            PackagedCliFixture.run(
                "--input-spec",
                inputFile.absolutePath,
                "--generator-name",
                "asyncapi-yaml",
                "--output-file",
                outputFile.absolutePath,
            )

        assertEquals(0, result.exitCode, result.output)
        assertTrue(result.output.contains("Generation complete."))
        assertTrue(outputFile.exists())
        assertTrue(outputFile.readText().startsWith("asyncapi:"))
    }

    @Test
    fun `should return a failure status for malformed input`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/diagnostics/asyncapi-malformed.yaml")
        val outputFile = tempDir.resolve("should-not-be-generated.yaml").toFile()

        val result =
            PackagedCliFixture.run(
                "--input-spec",
                inputFile.absolutePath,
                "--generator-name",
                "asyncapi-yaml",
                "--output-file",
                outputFile.absolutePath,
            )

        assertEquals(1, result.exitCode)
        assertTrue(result.output.contains("Malformed input document: ${inputFile.absolutePath}"))
        assertFalse(result.output.contains("DocumentReadException"))
        assertFalse(result.output.contains("\tat "))
        assertFalse(outputFile.exists())
    }
}
