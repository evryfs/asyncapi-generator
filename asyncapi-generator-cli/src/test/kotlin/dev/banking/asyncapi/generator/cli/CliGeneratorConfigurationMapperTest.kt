package dev.banking.asyncapi.generator.cli

import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorProfile
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile

class CliGeneratorConfigurationMapperTest {
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
    fun `maps the complete Spring Kafka client configuration`() {
        val configuration =
            mapConfiguration(
                generatorName = "kotlin",
                modelPackage = "com.example.model",
                clientPackage = "com.example.client",
                clientConfig =
                    CliClientConfiguration(
                        clientType = "spring-kafka",
                        clientContract = "interface",
                        producer = CliProducerConfiguration(enabled = false),
                        topicParameterProperties =
                            mapOf(
                                "environment" to "kafka.environment",
                            ),
                        validationAnnotations =
                            CliValidationAnnotationsConfiguration(
                                clientContract = "org.springframework.validation.annotation.Validated",
                                payloadParameter = "jakarta.validation.Valid",
                            ),
                    ),
            )

        val kafka = configuration.clients.single() as ClientGeneration.Kafka
        val springKafka = requireNotNull(kafka.springKafka)

        assertFalse(springKafka.producer.enabled)
        assertTrue(springKafka.consumer.enabled)
        assertEquals(
            mapOf("environment" to "kafka.environment"),
            springKafka.topicParameterProperties.mappings,
        )
        assertEquals(
            "org.springframework.validation.annotation.Validated",
            springKafka.validationAnnotations.clientContract?.value,
        )
        assertEquals(
            "jakarta.validation.Valid",
            springKafka.validationAnnotations.payloadParameter?.value,
        )
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
                    CliModelConfiguration(
                        modelType = "avro-specific-record",
                    ),
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
        modelConfig: CliModelConfiguration? = null,
        clientConfig: CliClientConfiguration? = null,
    ) = CliGeneratorConfigurationMapper.map(
        CliGeneratorConfigurationRequest(
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
