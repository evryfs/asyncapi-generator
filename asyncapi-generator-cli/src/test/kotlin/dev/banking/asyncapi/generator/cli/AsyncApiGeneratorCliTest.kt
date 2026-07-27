package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.parse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText
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
        assertTrue(help.contains("--version"))
        assertTrue(help.contains("Generate Kotlin models and Spring Kafka contracts"))
    }

    @Test
    fun `should generate kotlin code from valid input`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        cli.parse(
            arrayOf(
                "--input-spec", inputFile.absolutePath,
                "--output-directory", outputDirectory.absolutePath,
                "--model-package", "com.example.cli.model",
                "--client-package", "com.example.cli.client",
                "--client-type", "spring-kafka",
                "--client-contract", "interface",
                "--generator-name", "kotlin",
            )
        )
        val packageDir = outputDirectory.resolve("com/example/cli/client")
        assertTrue(packageDir.exists(), "Output package directory should exist")
        assertTrue(packageDir.list()?.isNotEmpty() == true, "Output directory should contain generated files")
    }

    @Test
    fun `should generate client and model outputs from shared package configuration`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        cli.parse(
            arrayOf(
                "--input-spec", inputFile.absolutePath,
                "--output-directory", outputDirectory.absolutePath,
                "--model-package", "com.example.cli.model",
                "--client-package", "com.example.cli.client",
                "--client-type", "spring-kafka",
                "--client-contract", "interface",
                "--generator-name", "kotlin",
            )
        )
        val clientDir = outputDirectory.resolve("com/example/cli/client")
        val modelDir = outputDirectory.resolve("com/example/cli/model")
        assertTrue(clientDir.exists(), "Client output directory should exist")
        assertTrue(modelDir.exists(), "Model output directory should exist")
    }

    @Test
    fun `should apply complete consumer client configuration from CLI options`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_spring_kafka.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()

        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "-g", "kotlin",
                "-o", outputDirectory.absolutePath,
                "--model-package", "com.example.cli.model",
                "--client-package", "com.example.cli.client",
                "--client-type", "spring-kafka",
                "--client-contract", "interface",
                "--topic-parameter-property", "environment=kafka.environment",
                "--client-contract-validation-annotation",
                "org.springframework.validation.annotation.Validated",
                "--payload-parameter-validation-annotation", "jakarta.validation.Valid",
                "--no-generate-producer",
            ),
        )

        val clientDirectory = outputDirectory.resolve("com/example/cli/client")
        val consumerFile = clientDirectory.resolve("consumer/MyAccountUpdatedConsumer.kt")
        val producerFile = clientDirectory.resolve("producer/MyAccountUpdatedProducer.kt")

        assertTrue(consumerFile.exists(), "Consumer contract should be generated")
        assertFalse(producerFile.exists(), "Producer contract should be disabled")
        val consumer = consumerFile.readText()
        assertTrue(consumer.contains("import jakarta.validation.Valid"))
        assertTrue(consumer.contains("import org.springframework.validation.annotation.Validated"))
        assertTrue(consumer.contains("@Validated"))
        assertTrue(consumer.contains("@Valid"))
        assertTrue(consumer.contains("my.accounts.\\${'$'}{kafka.environment}.updated.v1"))
    }

    @Test
    fun `should allow consumer generation to be disabled`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_spring_kafka.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()

        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "-g", "kotlin",
                "-o", outputDirectory.absolutePath,
                "--model-package", "com.example.cli.model",
                "--client-package", "com.example.cli.client",
                "--client-type", "spring-kafka",
                "--client-contract", "interface",
                "--topic-parameter-property", "environment=kafka.environment",
                "--no-generate-consumer",
            ),
        )

        val clientDirectory = outputDirectory.resolve("com/example/cli/client")
        assertTrue(
            clientDirectory.resolve("producer/MyAccountUpdatedProducer.kt").exists(),
            "Producer contract should be generated",
        )
        assertFalse(
            clientDirectory.resolve("consumer/MyAccountUpdatedConsumer.kt").exists(),
            "Consumer contract should be disabled",
        )
    }

    @Test
    fun `should reject incomplete client configuration before reading the contract`(@TempDir tempDir: Path) {
        val invalidInput = tempDir.resolve("invalid.yaml")
        invalidInput.writeText("not: [valid")

        val exception =
            assertFailsWith<UsageError> {
                cli.parse(
                    arrayOf(
                        "-i", invalidInput.toFile().absolutePath,
                        "-g", "kotlin",
                        "--model-package", "com.example.cli.model",
                        "--client-package", "com.example.cli.client",
                        "--client-type", "spring-kafka",
                    ),
                )
            }

        assertTrue(exception.message.orEmpty().contains("clientConfig.clientContract is required"))
    }

    @Test
    fun `should generate java code from valid input`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--output-directory", outputDirectory.absolutePath,
                "--model-package", "com.example.cli.model",
                "-g", "java"
            )
        )
        val packageDir = outputDirectory.resolve("com/example/cli/model")
        assertTrue(packageDir.exists(), "Java output directory should exist")
        assertTrue(packageDir.list()?.isNotEmpty() == true, "Output should not be empty")
    }

    @Test
    fun `should accept java record model type for java model generation`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--output-directory", outputDirectory.absolutePath,
                "--model-package", "com.example.cli.model",
                "--model-type", "java-record",
                "-g", "java",
            )
        )

        val packageDir = outputDirectory.resolve("com/example/cli/model")
        assertTrue(packageDir.exists(), "Java output directory should exist")
        val generatedRecord = packageDir.resolve("User.java")
        assertTrue(generatedRecord.readText().contains("public record User("))
    }

    @Test
    fun `should generate Avro schemas through the schema-only profile`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--output-directory", outputDirectory.absolutePath,
                "--schema-package", "com.example.cli.schema",
                "-g", "avro-schema",
            )
        )
        val schemaDir = outputDirectory.resolve("com/example/cli/schema")
        assertTrue(schemaDir.exists(), "Schema output directory should exist")
        assertTrue(schemaDir.list()?.isNotEmpty() == true, "Schema directory should not be empty")
    }

    @Test
    fun `should generate JSON Schemas through the schema-only profile`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()

        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--output-directory", outputDirectory.absolutePath,
                "--schema-package", "com.example.cli.schema",
                "-g", "json-schema",
            ),
        )

        val schemaFile = outputDirectory.resolve("com/example/cli/schema/User.schema.json")
        assertTrue(schemaFile.exists(), "JSON Schema output should exist")
        assertTrue(schemaFile.readText().contains("\"${'$'}schema\" : \"http://json-schema.org/draft-07/schema#\""))
        assertFalse(
            outputDirectory.walkTopDown().any { it.extension == "java" || it.extension == "kt" },
            "JSON Schema-only generation must not create source models",
        )
    }

    @Test
    fun `should generate native avro schema and specific record source`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_native_avro.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--output-directory", outputDirectory.absolutePath,
                "--model-package", "com.example.avro",
                "--model-type", "avro-specific-record",
                "-g", "java",
            )
        )

        val schemaFile = outputDirectory.resolve("com/example/avro/UserCreated.avsc")
        val specificRecordFile = outputDirectory.resolve("com/example/avro/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Avro schema output should exist")
        assertTrue(specificRecordFile.exists(), "SpecificRecord source output should exist")
        assertTrue(specificRecordFile.readText().contains("extends org.apache.avro.specific.SpecificRecordBase"))
    }

    @Test
    fun `should generate native protobuf schema and Java message types by default`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_native_protobuf.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--output-directory", outputDirectory.absolutePath,
                "--model-package", "com.example.protobuf",
                "--model-type", "protobuf-message",
                "-g", "java",
            )
        )

        val schemaFile = outputDirectory.resolve("com/example/protobuf/UserCreated.proto")
        val javaMessageFile = outputDirectory.resolve("com/example/protobuf/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Protobuf schema output should exist")
        assertTrue(schemaFile.readText().contains("message UserCreated"))
        assertTrue(javaMessageFile.exists(), "Native Protobuf Java message output should exist")
        assertTrue(javaMessageFile.readText().contains("public final class UserCreated"))
    }

    @Test
    fun `should generate native protobuf schema without model generation when model package is omitted`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_native_protobuf.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--output-directory", outputDirectory.absolutePath,
                "--schema-package", "com.example.protobuf",
                "-g", "protobuf-schema",
            )
        )

        val schemaFile = outputDirectory.resolve("com/example/protobuf/UserCreated.proto")
        val javaMessageFile = outputDirectory.resolve("com/example/protobuf/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Protobuf schema output should exist")
        assertFalse(javaMessageFile.exists(), "Native Protobuf Java message output should not exist")
    }

    @Test
    fun `should generate native protobuf Java messages and Kotlin DSL`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_native_protobuf.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--output-directory", outputDirectory.absolutePath,
                "--model-package", "com.example.protobuf",
                "--model-type", "protobuf-message",
                "-g", "kotlin",
            )
        )

        val schemaFile = outputDirectory.resolve("com/example/protobuf/UserCreated.proto")
        val javaMessageFile = outputDirectory.resolve("com/example/protobuf/UserCreated.java")
        val kotlinDslFile = outputDirectory.resolve("com/example/protobuf/UserCreatedKt.kt")
        assertTrue(schemaFile.exists(), "Native Protobuf schema output should exist")
        assertTrue(javaMessageFile.exists(), "Required Protobuf Java message output should exist")
        assertTrue(kotlinDslFile.exists(), "Protobuf Kotlin DSL output should exist")
    }

    @Test
    fun `should write bundled YAML through the AsyncAPI YAML profile`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputFile = tempDir.resolve("bundled.yaml").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--output-file", outputFile.absolutePath,
                "--generator-name", "asyncapi-yaml",
            )
        )
        assertTrue(outputFile.exists(), "Bundled output file should exist")
        assertTrue(outputFile.readText().startsWith("asyncapi:"))
    }

    @Test
    fun `should write bundled JSON through the AsyncAPI JSON profile`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputFile = tempDir.resolve("bundled.json").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--output-file", outputFile.absolutePath,
                "--generator-name", "asyncapi-json",
            )
        )
        assertTrue(outputFile.exists(), "Bundled output file should exist")
        assertTrue(outputFile.readText().startsWith("{"))
        assertTrue(outputFile.readText().contains("\"asyncapi\""))
    }

    @Test
    fun `should reject document profile without output file`() {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val exception =
            assertFailsWith<UsageError> {
                cli.parse(
                    arrayOf(
                        "-i", inputFile.absolutePath,
                        "--generator-name", "asyncapi-json",
                    ),
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                "outputFile is required when generatorName is asyncapi-json",
            ),
        )
    }

    @Test
    fun `should fail if kafka spring kafka client is enabled without client package`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        val exception =
            assertFailsWith<UsageError> {
                cli.parse(
                    arrayOf(
                        "-i", inputFile.absolutePath,
                        "--output-directory", outputDirectory.absolutePath,
                        "--model-package", "com.example.cli.model",
                        "--client-type", "spring-kafka",
                        "--client-contract", "interface",
                        "--generator-name", "kotlin",
                    )
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                "clientPackage is required when clientConfig is configured",
            ),
        )
    }

    @Test
    fun `should fail if schema generator is used without schema package`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        val exception =
            assertFailsWith<UsageError> {
                cli.parse(
                    arrayOf(
                        "-i", inputFile.absolutePath,
                        "--output-directory", outputDirectory.absolutePath,
                        "--generator-name", "avro-schema",
                    )
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                "schemaPackage is required when generatorName is avro-schema",
            ),
        )
    }

    @Test
    fun `should fail if model annotation is set without model package`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        val exception =
            assertFailsWith<UsageError> {
                cli.parse(
                    arrayOf(
                        "-i", inputFile.absolutePath,
                        "--output-directory", outputDirectory.absolutePath,
                        "--model-annotation", "com.example.NoArg",
                        "--generator-name", "kotlin",
                    )
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                "models.packageName is required when models.annotation is configured",
            ),
        )
    }

    @Test
    fun `should fail if model type is invalid`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        assertFailsWith<BadParameterValue> {
            cli.parse(
                arrayOf(
                    "-i", inputFile.absolutePath,
                    "--output-directory", outputDirectory.absolutePath,
                    "--model-package", "com.example.cli.model",
                    "--model-type", "data",
                    "-g", "java",
                )
            )
        }
    }

    @Test
    fun `should fail if java record model type is configured for kotlin`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        val exception =
            assertFailsWith<UsageError> {
                cli.parse(
                    arrayOf(
                        "-i", inputFile.absolutePath,
                        "--output-directory", outputDirectory.absolutePath,
                        "--model-package", "com.example.cli.model",
                        "--model-type", "java-record",
                        "-g", "kotlin",
                    )
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                "modelConfig.modelType 'java-record' is not supported when generatorName is kotlin",
            ),
        )
    }

    @Test
    fun `should fail if input file is missing`(@TempDir tempDir: Path) {
        val outputDirectory = tempDir.resolve("generated").toFile()
        assertFailsWith<UsageError> {
            cli.parse(
                arrayOf(
                    "-i", "non_existent.yaml",
                    "--output-directory", outputDirectory.absolutePath,
                    "--model-package", "com.example.cli.model",
                    "-g", "kotlin",
                )
            )
        }
    }

    @Test
    fun `should fail if generator name is invalid`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val outputDirectory = tempDir.resolve("generated").toFile()
        assertFailsWith<BadParameterValue> {
            cli.parse(
                arrayOf(
                    "-i", inputFile.absolutePath,
                    "--output-directory", outputDirectory.absolutePath,
                    "--model-package", "com.example.cli.model",
                    "-g", "invalid-gen"
                )
            )
        }
    }
}
