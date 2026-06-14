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
import kotlin.test.assertFailsWith

class AsyncApiGeneratorCliTest {

    private val cli = AsyncApiGeneratorCli()

    @Test
    fun `should generate kotlin code from valid input`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        val resourceDir = tempDir.resolve("resources").toFile()
        cli.parse(
            arrayOf(
                "--input", inputFile.absolutePath,
                "--codegen-output", codegenDir.absolutePath,
                "--resource-output", resourceDir.absolutePath,
                "--models-package", "com.example.cli.model",
                "--clients-spring-kafka-package", "com.example.cli.client",
                "--generator", "kotlin",
                "--clients-spring-kafka-mode", "full",
            )
        )
        val packageDir = codegenDir.resolve("src/main/kotlin/com/example/cli/client")
        assertTrue(packageDir.exists(), "Output package directory should exist")
        assertTrue(packageDir.list()?.isNotEmpty() == true, "Output directory should contain generated files")
    }

    @Test
    fun `should generate client with explicit model package when models are not generated`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        val resourceDir = tempDir.resolve("resources").toFile()
        cli.parse(
            arrayOf(
                "--input", inputFile.absolutePath,
                "--codegen-output", codegenDir.absolutePath,
                "--resource-output", resourceDir.absolutePath,
                "--clients-spring-kafka-package", "com.example.cli.client",
                "--clients-spring-kafka-model-package", "com.example.cli.model",
                "--generator", "kotlin",
            )
        )
        val clientDir = codegenDir.resolve("src/main/kotlin/com/example/cli/client")
        val modelDir = codegenDir.resolve("src/main/kotlin/com/example/cli/model")
        assertTrue(clientDir.exists(), "Client output directory should exist")
        assertTrue(!modelDir.exists(), "Model output directory should not exist when models are not generated")
    }

    @Test
    fun `should generate java code from valid input`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--codegen-output", codegenDir.absolutePath,
                "--models-package", "com.example.cli.model",
                "-g", "java"
            )
        )
        val packageDir = codegenDir.resolve("src/main/java/com/example/cli/model")
        assertTrue(packageDir.exists(), "Java output directory should exist")
        assertTrue(packageDir.list()?.isNotEmpty() == true, "Output should not be empty")
    }

    @Test
    fun `should accept java record model type for java model generation`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--codegen-output", codegenDir.absolutePath,
                "--models-package", "com.example.cli.model",
                "--models-java-model-type", "record",
                "-g", "java",
            )
        )

        val packageDir = codegenDir.resolve("src/main/java/com/example/cli/model")
        assertTrue(packageDir.exists(), "Java output directory should exist")
        val generatedRecord = packageDir.resolve("User.java")
        assertTrue(generatedRecord.readText().contains("public record User("))
    }

    @Test
    fun `should generate avro schema when schema mode is avro projection`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        val resourceDir = tempDir.resolve("resources").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--codegen-output", codegenDir.absolutePath,
                "--resource-output", resourceDir.absolutePath,
                "--models-package", "com.example.cli.model",
                "--schemas-avro-projection-package", "com.example.cli.schema",
                "-g", "kotlin",
            )
        )
        val schemaDir = resourceDir.resolve("com/example/cli/schema")
        assertTrue(schemaDir.exists(), "Schema output directory should exist")
        assertTrue(schemaDir.list()?.isNotEmpty() == true, "Schema directory should not be empty")
    }

    @Test
    fun `should generate native avro schema and specific record source`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_native_avro.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        val resourceDir = tempDir.resolve("resources").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--codegen-output", codegenDir.absolutePath,
                "--resource-output", resourceDir.absolutePath,
                "--schemas-native-avro",
                "-g", "kotlin",
            )
        )

        val schemaFile = resourceDir.resolve("com/example/avro/UserCreated.avsc")
        val specificRecordFile = codegenDir.resolve("src/main/java/com/example/avro/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Avro schema output should exist")
        assertTrue(specificRecordFile.exists(), "SpecificRecord source output should exist")
        assertTrue(specificRecordFile.readText().contains("extends org.apache.avro.specific.SpecificRecordBase"))
    }

    @Test
    fun `should generate native protobuf schema`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_native_protobuf.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        val resourceDir = tempDir.resolve("resources").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--codegen-output", codegenDir.absolutePath,
                "--resource-output", resourceDir.absolutePath,
                "--schemas-native-protobuf",
                "-g", "kotlin",
            )
        )

        val schemaFile = resourceDir.resolve("com/example/protobuf/UserCreated.proto")
        val javaMessageFile = codegenDir.resolve("src/main/java/com/example/protobuf/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Protobuf schema output should exist")
        assertTrue(schemaFile.readText().contains("message UserCreated"))
        assertTrue(javaMessageFile.exists(), "Native Protobuf Java message output should exist")
        assertTrue(javaMessageFile.readText().contains("public final class UserCreated"))
    }

    @Test
    fun `should generate native protobuf schema without Java message types when disabled`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_native_protobuf.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        val resourceDir = tempDir.resolve("resources").toFile()
        cli.parse(
            arrayOf(
                "-i", inputFile.absolutePath,
                "--codegen-output", codegenDir.absolutePath,
                "--resource-output", resourceDir.absolutePath,
                "--schemas-native-protobuf",
                "--schemas-native-protobuf-generate-java-message-types", "false",
                "-g", "kotlin",
            )
        )

        val schemaFile = resourceDir.resolve("com/example/protobuf/UserCreated.proto")
        val javaMessageFile = codegenDir.resolve("src/main/java/com/example/protobuf/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Protobuf schema output should exist")
        assertFalse(javaMessageFile.exists(), "Native Protobuf Java message output should not exist")
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
    fun `should fail if spring kafka client is enabled without client package`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        val exception =
            assertFailsWith<UsageError> {
                cli.parse(
                    arrayOf(
                        "-i", inputFile.absolutePath,
                        "--codegen-output", codegenDir.absolutePath,
                        "--models-package", "com.example.cli.model",
                        "--clients-spring-kafka",
                    )
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                "clients.springKafka.packageName is required when clients.springKafka is configured",
            ),
        )
    }

    @Test
    fun `should fail if avro projection is enabled without schema package`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        val exception =
            assertFailsWith<UsageError> {
                cli.parse(
                    arrayOf(
                        "-i", inputFile.absolutePath,
                        "--codegen-output", codegenDir.absolutePath,
                        "--models-package", "com.example.cli.model",
                        "--schemas-avro-projection",
                    )
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                "schemas.avroProjection.packageName is required when schemas.avroProjection is configured",
            ),
        )
    }

    @Test
    fun `should fail if model annotation is set without model package`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        val exception =
            assertFailsWith<UsageError> {
                cli.parse(
                    arrayOf(
                        "-i", inputFile.absolutePath,
                        "--codegen-output", codegenDir.absolutePath,
                        "--models-annotation", "com.example.NoArg",
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
    fun `should fail if spring kafka mode is invalid`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        assertFailsWith<BadParameterValue> {
            cli.parse(
                arrayOf(
                    "-i", inputFile.absolutePath,
                    "--codegen-output", codegenDir.absolutePath,
                    "--models-package", "com.example.cli.model",
                    "-g", "kotlin",
                    "--clients-spring-kafka-package", "com.example.cli.client",
                    "--clients-spring-kafka-mode", "basic",
                )
            )
        }
    }

    @Test
    fun `should fail if java model type is invalid`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        assertFailsWith<BadParameterValue> {
            cli.parse(
                arrayOf(
                    "-i", inputFile.absolutePath,
                    "--codegen-output", codegenDir.absolutePath,
                    "--models-package", "com.example.cli.model",
                    "--models-java-model-type", "data",
                    "-g", "java",
                )
            )
        }
    }

    @Test
    fun `should fail if java record model type is configured for kotlin`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        val exception =
            assertFailsWith<UsageError> {
                cli.parse(
                    arrayOf(
                        "-i", inputFile.absolutePath,
                        "--codegen-output", codegenDir.absolutePath,
                        "--models-package", "com.example.cli.model",
                        "--models-java-model-type", "record",
                        "-g", "kotlin",
                    )
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                "models.javaModelType=record is only supported when generatorName is java",
            ),
        )
    }

    @Test
    fun `should fail if input file is missing`(@TempDir tempDir: Path) {
        val codegenDir = tempDir.resolve("codegen").toFile()
        assertFailsWith<UsageError> {
            cli.parse(
                arrayOf(
                    "-i", "non_existent.yaml",
                    "--codegen-output", codegenDir.absolutePath,
                    "--models-package", "com.example.cli.model"
                )
            )
        }
    }

    @Test
    fun `should fail if generator name is invalid`(@TempDir tempDir: Path) {
        val inputFile = File("src/test/resources/asyncapi_kafka_complex.yaml")
        val codegenDir = tempDir.resolve("codegen").toFile()
        assertFailsWith<BadParameterValue> {
            cli.parse(
                arrayOf(
                    "-i", inputFile.absolutePath,
                    "--codegen-output", codegenDir.absolutePath,
                    "--models-package", "com.example.cli.model",
                    "-g", "invalid-gen"
                )
            )
        }
    }
}
