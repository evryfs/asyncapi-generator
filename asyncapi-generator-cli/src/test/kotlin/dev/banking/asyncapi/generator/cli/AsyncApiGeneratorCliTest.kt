package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AsyncApiGeneratorCliTest {
    private val cli = AsyncApiGeneratorCli()

    @Test
    fun `should describe the supported generation workflows in command help`() {
        val help = requireNotNull(AsyncApiGeneratorCli().getFormattedHelp())

        assertTrue(help.contains("Generate payload models, client contracts, schema artifacts"))
        assertTrue(help.contains("Generation"))
        assertTrue(help.contains("Outputs"))
        assertTrue(help.contains("Models"))
        assertTrue(help.contains("Clients"))
        assertTrue(help.contains("--generator-name"))
        assertTrue(help.contains("--topic-parameter-property"))
        assertTrue(help.contains("--producer-additional-payload-type"))
        assertTrue(help.contains("--version"))
        assertTrue(help.contains("--generate-completion"))
        assertTrue(help.contains("Generate Kotlin models and Spring Kafka contracts"))
    }

    @Test
    fun `should generate shell completion from the typed option contract`() {
        val result =
            AsyncApiGeneratorCli().test(
                arrayOf("--generate-completion", "zsh"),
            )

        assertEquals(0, result.statusCode, result.output)
        assertTrue(result.stdout.contains("asyncapi-generator"))
        assertTrue(result.stdout.contains("--generator-name"))
        assertTrue(result.stdout.contains("--topic-parameter-property"))
    }

    @Test
    fun `should reject invalid client configuration before reading the contract`(
        @TempDir tempDir: Path,
    ) {
        val invalidInput = tempDir.resolve("invalid.yaml")
        invalidInput.writeText("not: [valid")

        val exception =
            assertFailsWith<UsageError> {
                cli.parse(
                    arrayOf(
                        "-i",
                        invalidInput.toFile().absolutePath,
                        "-g",
                        "kotlin",
                        "--model-package",
                        "com.example.cli.model",
                        "--client-package",
                        "com.example.cli.client",
                        "--topic-parameter-property",
                        "environment=${'$'}{kafka.environment}",
                    ),
                )
            }

        assertTrue(
            exception.message.orEmpty().contains("clientConfig.topicParameterProperties.environment must be"),
            exception.message,
        )
    }

    @Test
    fun `should generate java code from valid input`(
        @TempDir tempDir: Path,
    ) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        cli.parse(
            arrayOf(
                "-i",
                inputFile.absolutePath,
                "--output-directory",
                outputDirectory.absolutePath,
                "--model-package",
                "com.example.cli.model",
                "-g",
                "java",
            ),
        )
        val packageDir = outputDirectory.resolve("com/example/cli/model")
        assertTrue(packageDir.exists(), "Java output directory should exist")
        assertTrue(packageDir.list()?.isNotEmpty() == true, "Output should not be empty")
    }
}
