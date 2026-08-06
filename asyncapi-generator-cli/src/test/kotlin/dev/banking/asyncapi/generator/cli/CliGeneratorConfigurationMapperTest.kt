package dev.banking.asyncapi.generator.cli

import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
                        producer =
                            CliProducerConfiguration(
                                enabled = false,
                                additionalPayloadTypes = listOf("string", "byte-array"),
                            ),
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
        assertEquals(
            listOf(
                AdditionalProducerPayloadType.BYTE_ARRAY,
                AdditionalProducerPayloadType.STRING,
            ),
            springKafka.producer.additionalPayloadTypes.toList(),
        )
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
        clientConfig: CliClientConfiguration? = null,
    ) = CliGeneratorConfigurationMapper.map(
        CliGeneratorConfigurationRequest(
            generatorName = generatorName,
            outputDirectory = outputDirectory,
            outputFile = outputFile,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            clientConfig = clientConfig,
        ),
    )
}
