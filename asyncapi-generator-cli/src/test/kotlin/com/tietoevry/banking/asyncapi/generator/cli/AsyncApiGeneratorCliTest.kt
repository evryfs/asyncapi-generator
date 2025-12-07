package com.tietoevry.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.parse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFailsWith

class AsyncApiGeneratorCliTest {

    private val cli = AsyncApiGeneratorCli()

    @Test
    fun `should generate kotlin code from valid input`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDir = tempDir.toFile()
        cli.parse(
            arrayOf(
                "--input", inputFile.absolutePath,
                "--output", outputDir.absolutePath,
                "--model-package", "com.example.cli.model",
                "--client-package", "com.example.cli.client",
                "--generator", "kotlin",
                "--spring-kafka"
            )
        )
        val packageDir = outputDir.resolve("src/main/kotlin/com/example/cli/client")
        assertTrue(packageDir.exists(), "Output package directory should exist")
        assertTrue(packageDir.list()?.isNotEmpty() == true, "Output directory should contain generated files")
    }

    @Test
    fun `should generate java code from valid input`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDir = tempDir.toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "-o", outputDir.absolutePath,
                "--model-package", "com.example.cli.model",
                "-g", "java"
            )
        )
        val packageDir = outputDir.resolve("src/main/java/com/example/cli/model")
        assertTrue(packageDir.exists(), "Java output directory should exist")
        assertTrue(packageDir.list()?.isNotEmpty() == true, "Output should not be empty")
    }

    @Test
    fun `should fail if input file is missing`(@TempDir tempDir: Path) {
        val outputDir = tempDir.toFile()
        assertFailsWith<UsageError> {
            cli.parse(
                arrayOf(
                    "-i", "non_existent.yaml", // Missing file
                    "-o", outputDir.absolutePath,
                    "--model-package", "com.example.cli.model"
                )
            )
        }
    }

    @Test
    fun `should fail if model package is not provided`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDir = tempDir.toFile()
        assertFailsWith<MissingOption> {
            cli.parse(
                arrayOf(
                    "-i", inputFile.absolutePath,
                    "-o", outputDir.absolutePath,
                    "-g", "kotlin"
                )
            )
        }
    }

    @Test
    fun `should fail if generator name is invalid`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDir = tempDir.toFile()
        assertFailsWith<BadParameterValue> {
            cli.parse(
                arrayOf(
                    "-i", inputFile.absolutePath,
                    "-o", outputDir.absolutePath,
                    "--model-package", "com.example.cli.model",
                    "-g", "invalid-gen"
                )
            )
        }
    }
}
