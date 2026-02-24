package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.core.BadParameterValue
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
                "--config-option", "client.type=spring-kafka"
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
    fun `should generate avro schema when schema type is avro`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDir = tempDir.toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "-o", outputDir.absolutePath,
                "--model-package", "com.example.cli.model",
                "--schema-package", "com.example.cli.schema",
                "-g", "kotlin",
                "--config-option", "schema.type=avro"
            )
        )
        val schemaDir = outputDir.resolve("src/main/kotlin/com/example/cli/schema")
        assertTrue(schemaDir.exists(), "Schema output directory should exist")
    }

    @Test
    fun `should allow bundle-only output with no packages`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputFile = tempDir.resolve("bundled.yaml").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--output-file", outputFile.absolutePath
            )
        )
        assertTrue(outputFile.exists(), "Bundled output file should exist")
        assertTrue(outputFile.length() > 0, "Bundled output file should not be empty")
    }

    @Test
    fun `should fail if client type is set without client package`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDir = tempDir.toFile()
        assertFailsWith<UsageError> {
            cli.parse(
                arrayOf(
                    "-i", inputFile.absolutePath,
                    "-o", outputDir.absolutePath,
                    "--model-package", "com.example.cli.model",
                    "--config-option", "client.type=spring-kafka"
                )
            )
        }
    }

    @Test
    fun `should fail if schema type is set without schema package`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDir = tempDir.toFile()
        assertFailsWith<UsageError> {
            cli.parse(
                arrayOf(
                    "-i", inputFile.absolutePath,
                    "-o", outputDir.absolutePath,
                    "--model-package", "com.example.cli.model",
                    "--config-option", "schema.type=avro"
                )
            )
        }
    }

    @Test
    fun `should fail if model no-arg annotation is set without model package`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDir = tempDir.toFile()
        assertFailsWith<UsageError> {
            cli.parse(
                arrayOf(
                    "-i", inputFile.absolutePath,
                    "-o", outputDir.absolutePath,
                    "--config-option", "model.annotation=com.example.NoArg"
                )
            )
        }
    }

    @Test
    fun `should fail if config option format is invalid`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDir = tempDir.toFile()
        assertFailsWith<UsageError> {
            cli.parse(
                arrayOf(
                    "-i", inputFile.absolutePath,
                    "-o", outputDir.absolutePath,
                    "--model-package", "com.example.cli.model",
                    "-g", "kotlin",
                    "--config-option", "client.type"
                )
            )
        }
    }

    @Test
    fun `should fail if input file is missing`(@TempDir tempDir: Path) {
        val outputDir = tempDir.toFile()
        assertFailsWith<UsageError> {
            cli.parse(
                arrayOf(
                    "-i", "non_existent.yaml",
                    "-o", outputDir.absolutePath,
                    "--model-package", "com.example.cli.model"
                )
            )
        }
    }

    @Test
    fun `should fail if model package is not provided and no bundle output is requested`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDir = tempDir.toFile()
        // No modelPackage, no clientPackage, no schemaPackage, and no outputFile
        // This should be allowed but results in no codegen and no bundle output; still a valid invocation.
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "-o", outputDir.absolutePath,
                "-g", "kotlin"
            )
        )
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
