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
    fun `should report the packaged CLI version`() {
        val result = PackagedCliFixture.run("--version")

        assertEquals(0, result.exitCode)
        assertEquals(
            "asyncapi-generator version ${PackagedCliFixture.expectedVersion}",
            result.stdout.trim(),
        )
    }

    @Test
    fun `should expose command help from the packaged CLI`() {
        val result = PackagedCliFixture.run("--help")

        assertEquals(0, result.exitCode)
        assertTrue(result.stderr.isBlank(), result.stderr)
        assertTrue(result.stdout.contains("Usage: asyncapi-generator"))
        assertTrue(result.stdout.contains("--input-spec"))
        assertTrue(result.stdout.contains("--generator-name"))
        assertTrue(result.stdout.contains("Run a repeatable generation request from an argument file"))
        assertTrue(result.stdout.contains("@generation.args"))
    }

    @Test
    fun `should generate shell completion from the packaged CLI`() {
        val result = PackagedCliFixture.run("--generate-completion", "bash")

        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("asyncapi-generator"))
        assertTrue(result.stdout.contains("--generator-name"))
        assertTrue(result.stdout.contains("--topic-parameter-property"))
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
        assertTrue(result.stdout.contains("Generation complete."))
        assertTrue(outputFile.exists())
        assertTrue(outputFile.readText().startsWith("asyncapi:"))
    }

    @Test
    fun `should generate a bundled document from an argument file`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_spring_kafka.yaml")
        val outputFile = tempDir.resolve("bundled output.yaml").toFile()
        val argumentFile =
            tempDir.resolve("generation arguments.txt").toFile().apply {
                writeText(
                    """
                    # One option or value per line keeps repeatable invocations readable.
                    --input-spec
                    "${inputFile.absolutePath}"
                    --generator-name
                    asyncapi-yaml
                    --output-file
                    "${outputFile.absolutePath}"
                    """.trimIndent(),
                )
            }

        val result = PackagedCliFixture.run("@${argumentFile.absolutePath}")

        assertEquals(0, result.exitCode, result.output)
        assertTrue(result.stdout.contains("Generation complete."))
        assertTrue(outputFile.exists())
        assertTrue(outputFile.readText().startsWith("asyncapi:"))
    }

    @Test
    fun `should generate complete Spring Kafka output through the packaged CLI`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_spring_kafka.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()

        val result =
            PackagedCliFixture.run(
                "--input-spec",
                inputFile.absolutePath,
                "--generator-name",
                "kotlin",
                "--output-directory",
                outputDirectory.absolutePath,
                "--model-package",
                "com.example.cli.model",
                "--client-package",
                "com.example.cli.client",
                "--model-annotation",
                "com.example.codegen.GeneratedPayload",
                "--model-type",
                "kotlin-data-class",
                "--client-type",
                "spring-kafka",
                "--client-contract",
                "interface",
                "--generate-producer",
                "--generate-consumer",
                "--topic-parameter-property",
                "environment=kafka.environment",
                "--client-contract-validation-annotation",
                "org.springframework.validation.annotation.Validated",
                "--payload-parameter-validation-annotation",
                "jakarta.validation.Valid",
            )

        assertEquals(0, result.exitCode, result.output)
        val model =
            outputDirectory.resolve("com/example/cli/model/MyAccountUpdatedPayload.kt")
                .readText()
        val producer =
            outputDirectory.resolve("com/example/cli/client/producer/MyAccountUpdatedProducer.kt")
                .readText()
        val consumer =
            outputDirectory.resolve("com/example/cli/client/consumer/MyAccountUpdatedConsumer.kt")
                .readText()

        assertTrue(model.contains("import com.example.codegen.GeneratedPayload"))
        assertTrue(model.contains("@GeneratedPayload"))
        assertTrue(producer.contains("@Validated"))
        assertTrue(producer.contains("@Valid"))
        assertTrue(producer.contains("fun sendMyAccountUpdated("))
        assertTrue(producer.contains("CompletableFuture<RecordMetadata>"))
        assertTrue(consumer.contains("@Validated"))
        assertTrue(consumer.contains("@Valid"))
        assertTrue(consumer.contains("fun listenMyAccountUpdated("))
        assertTrue(consumer.contains("my.accounts.\\${'$'}{kafka.environment}.updated.v1"))
    }

    @Test
    fun `should generate native Avro output through the packaged CLI`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_native_avro.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()

        val result =
            PackagedCliFixture.run(
                "--input-spec",
                inputFile.absolutePath,
                "--generator-name",
                "java",
                "--output-directory",
                outputDirectory.absolutePath,
                "--model-package",
                "com.example.avro",
                "--schema-package",
                "com.example.avro.schema",
                "--model-type",
                "avro-specific-record",
            )

        assertEquals(0, result.exitCode, result.output)
        val schemaFile = outputDirectory.resolve("com/example/avro/schema/UserCreated.avsc")
        val specificRecordFile = outputDirectory.resolve("com/example/avro/UserCreated.java")

        assertTrue(schemaFile.isFile)
        assertTrue(schemaFile.readText().contains("\"type\" : \"record\""))
        assertTrue(specificRecordFile.isFile)
        assertTrue(
            specificRecordFile.readText()
                .contains("extends org.apache.avro.specific.SpecificRecordBase"),
        )
    }

    @Test
    fun `should generate native Protobuf output through the packaged CLI`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_native_protobuf.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()

        val result =
            PackagedCliFixture.run(
                "--input-spec",
                inputFile.absolutePath,
                "--generator-name",
                "kotlin",
                "--output-directory",
                outputDirectory.absolutePath,
                "--model-package",
                "com.example.protobuf",
                "--schema-package",
                "com.example.protobuf.schema",
                "--model-type",
                "protobuf-message",
            )

        assertEquals(0, result.exitCode, result.output)
        val schemaFile = outputDirectory.resolve("com/example/protobuf/schema/UserCreated.proto")
        val javaMessageFile = outputDirectory.resolve("com/example/protobuf/UserCreated.java")
        val kotlinDslFile = outputDirectory.resolve("com/example/protobuf/UserCreatedKt.kt")

        assertTrue(schemaFile.isFile)
        assertTrue(schemaFile.readText().contains("message UserCreated"))
        assertTrue(javaMessageFile.isFile)
        assertTrue(javaMessageFile.readText().contains("public final class UserCreated"))
        assertTrue(kotlinDslFile.isFile)
        assertTrue(kotlinDslFile.readText().contains("public object UserCreatedKt"))
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
        assertTrue(result.stderr.contains("Malformed input document: ${inputFile.absolutePath}"))
        assertFalse(result.stderr.contains("DocumentReadException"))
        assertFalse(result.stderr.contains("\tat "))
        assertFalse(outputFile.exists())
    }
}
