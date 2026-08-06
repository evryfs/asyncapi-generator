package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorProfile
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
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
    fun `maps additional producer payload types`() {
        val configuration =
            mapConfiguration(
                generatorName = "kotlin",
                modelPackage = "com.example.model",
                clientPackage = "com.example.client",
                clientConfig =
                    MavenTestHelper.clientConfig(
                        clientType = "spring-kafka",
                        clientContract = "interface",
                        producer =
                            MavenTestHelper.producer(
                                additionalPayloadTypes = listOf("string", "byte-array"),
                            ),
                    ),
            )

        val kafka = configuration.clients.single() as ClientGeneration.Kafka
        val producer = kafka.springKafka!!.producer

        assertEquals(
            listOf(
                AdditionalProducerPayloadType.BYTE_ARRAY,
                AdditionalProducerPayloadType.STRING,
            ),
            producer.additionalPayloadTypes.toList(),
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
        clientConfig: MavenClientConfiguration? = null,
    ) = MavenGeneratorConfigurationMapper.map(
        MavenGeneratorConfigurationRequest(
            generatorName = generatorName,
            outputDirectory = outputDirectory,
            outputFile = outputFile,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            clientConfig = clientConfig,
        ),
    )
}
