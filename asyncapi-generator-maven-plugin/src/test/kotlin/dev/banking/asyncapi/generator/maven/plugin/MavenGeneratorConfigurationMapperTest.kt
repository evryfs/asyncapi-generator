package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorProfile
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile

class MavenGeneratorConfigurationMapperTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `maps an explicit source generator and model package`() {
        val configuration =
            mapConfiguration(
                generatorName = "kotlin",
                modelPackage = "com.example.model",
            )

        assertEquals(
            GeneratorProfile.Source(SourceLanguage.KOTLIN),
            configuration.profile,
        )
        assertInstanceOf(ModelGeneration.Enabled::class.java, configuration.models)
    }

    @Test
    fun `requires an explicit generator name`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                mapConfiguration(
                    generatorName = null,
                    modelPackage = "com.example.model",
                )
            }

        assertEquals("generatorName is required", exception.message)
    }

    @Test
    fun `rejects source configuration without an activated output`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                mapConfiguration(generatorName = "kotlin")
            }

        assertEquals(
            "No generator output is configured. Configure modelPackage, clientPackage with clientConfig, " +
                "schemaPackage with a schema generator, or outputFile.",
            exception.message,
        )
    }

    @Test
    fun `requires client configuration when client package is configured`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                mapConfiguration(
                    generatorName = "kotlin",
                    modelPackage = "com.example.model",
                    clientPackage = "com.example.client",
                )
            }

        assertEquals("clientConfig is required when clientPackage is configured", exception.message)
    }

    @Test
    fun `rejects Spring Kafka configuration with both contracts disabled`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                mapConfiguration(
                    generatorName = "kotlin",
                    modelPackage = "com.example.model",
                    clientPackage = "com.example.client",
                    clientConfig =
                        MavenClientConfiguration().apply {
                            clientType = "spring-kafka"
                            clientContract = "interface"
                            producer =
                                MavenProducerConfiguration().apply {
                                    enabled = false
                                }
                            consumer =
                                MavenConsumerConfiguration().apply {
                                    enabled = false
                                }
                        },
                )
            }

        assertEquals(
            "Spring Kafka client generation requires at least one enabled contract: " +
                "producer.enabled or consumer.enabled",
            exception.message,
        )
    }

    @Test
    fun `rejects schema package when selected outputs do not generate schemas`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                mapConfiguration(
                    generatorName = "java",
                    modelPackage = "com.example.model",
                    schemaPackage = "com.example.schema",
                )
            }

        assertEquals(
            "schemaPackage is only supported by schema generator profiles and native Avro or Protobuf models",
            exception.message,
        )
    }

    @Test
    fun `accepts schema package for schema generator profile`() {
        val configuration =
            mapConfiguration(
                generatorName = "json-schema",
                schemaPackage = "com.example.schema",
            )

        assertTrue(configuration.schemas.single() is SchemaGeneration.JsonSchema)
    }

    @Test
    fun `accepts schema package for native model generation`() {
        val configuration =
            mapConfiguration(
                generatorName = "java",
                modelPackage = "com.example.avro",
                schemaPackage = "com.example.schema",
                modelConfig =
                    MavenModelConfiguration().apply {
                        modelType = "avro-specific-record"
                    },
            )

        val nativeAvro = configuration.schemas.single() as SchemaGeneration.NativeAvro
        assertEquals("com.example.schema", nativeAvro.schemaPackageName)
    }

    @Test
    fun `rejects output directory path that is a file`() {
        val outputFile = temporaryDirectory.resolve("output").createFile().toFile()
        val exception =
            assertThrows<IllegalArgumentException> {
                mapConfiguration(
                    generatorName = "kotlin",
                    modelPackage = "com.example.model",
                    outputDirectory = outputFile,
                )
            }

        assertEquals("outputDirectory must be a directory: $outputFile", exception.message)
    }

    @Test
    fun `rejects output file path that is a directory`() {
        val outputFile = temporaryDirectory.resolve("document").createDirectory().toFile()
        val exception =
            assertThrows<IllegalArgumentException> {
                mapConfiguration(
                    generatorName = "asyncapi-yaml",
                    outputFile = outputFile,
                )
            }

        assertEquals("outputFile must be a file: $outputFile", exception.message)
    }

    private fun mapConfiguration(
        generatorName: String?,
        outputDirectory: File = temporaryDirectory.resolve("generated").toFile(),
        outputFile: File? = null,
        modelPackage: String? = null,
        clientPackage: String? = null,
        schemaPackage: String? = null,
        modelConfig: MavenModelConfiguration? = null,
        clientConfig: MavenClientConfiguration? = null,
    ) = MavenGeneratorConfigurationMapper.map(
        MavenGeneratorConfigurationRequest(
            generatorName = generatorName,
            outputDirectory = outputDirectory,
            outputFile = outputFile,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            schemaPackage = schemaPackage,
            modelConfig = modelConfig,
            clientConfig = clientConfig,
        ),
    )
}
