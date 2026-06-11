package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorOutputConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.plan.SpringKafkaClientType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncApiGeneratorOutputContractTest {
    private val asyncApiContext = AsyncApiContext()
    private val bundlerFixtures = BundlerFixtures(asyncApiContext)
    private val generationInputFixtures = GenerationInputFixtures()
    private val generator = AsyncApiGenerator()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generate writes model artifacts to source output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val bundled = bundledDocument()

        generator.generate(
            asyncApiDocument = bundled,
            generatorConfiguration =
                generatorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    models = ModelGeneration.Enabled(packageName = "com.example.model"),
                ),
        )

        assertTrue(sourceOutputDirectory.resolve("com/example/model/Task.kt").exists())
        assertFalse(resourceOutputDirectory.resolve("com/example/model/Task.kt").exists())
    }

    @Test
    fun `generate writes schema artifacts to resource output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val bundled = bundledDocument()

        generator.generate(
            asyncApiDocument = bundled,
            generatorConfiguration =
                generatorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    schemas = listOf(SchemaGeneration.AvroProjection(packageName = "com.example.avro")),
                ),
        )

        assertTrue(resourceOutputDirectory.resolve("com/example/avro/Task.avsc").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/avro/Task.avsc").exists())
    }

    @Test
    fun `generate rejects multi format component schemas before writing model artifacts`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                generator.generate(
                    asyncApiDocument = generationInputFixtures.documentWithMultiFormatComponent(),
                    generatorConfiguration =
                        generatorConfiguration(
                            sourceOutputDirectory = sourceOutputDirectory,
                            resourceOutputDirectory = resourceOutputDirectory,
                            models = ModelGeneration.Enabled(packageName = "com.example.model"),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Model generation cannot consume payload 'UserCreated'"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }

    @Test
    fun `generate rejects multi format component schemas before writing avro projection artifacts`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                generator.generate(
                    asyncApiDocument = generationInputFixtures.documentWithMultiFormatComponent(),
                    generatorConfiguration =
                        generatorConfiguration(
                            sourceOutputDirectory = sourceOutputDirectory,
                            resourceOutputDirectory = resourceOutputDirectory,
                            schemas = listOf(SchemaGeneration.AvroProjection(packageName = "com.example.avro")),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Avro Projection cannot consume payload 'UserCreated'"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }

    @Test
    fun `generate rejects multi format message payloads before writing spring kafka artifacts`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                generator.generate(
                    asyncApiDocument = generationInputFixtures.documentWithMultiFormatMessagePayload(),
                    generatorConfiguration =
                        generatorConfiguration(
                            sourceOutputDirectory = sourceOutputDirectory,
                            resourceOutputDirectory = resourceOutputDirectory,
                            clients =
                                listOf(
                                    ClientGeneration.SpringKafka(
                                        packageName = "com.example.kafka",
                                        modelPackageName = "com.example.model",
                                        clientType = SpringKafkaClientType.SIMPLE,
                                    ),
                                ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Spring Kafka client generation cannot consume payload 'UserCreatedPayload'"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }

    private fun bundledDocument() =
        bundlerFixtures.bundledDocument(
            File("src/test/resources/generator/asyncapi_enum_default_value.yaml"),
        )

    private fun generatorConfiguration(
        sourceOutputDirectory: File,
        resourceOutputDirectory: File,
        models: ModelGeneration = ModelGeneration.Disabled,
        schemas: List<SchemaGeneration> = emptyList(),
        clients: List<ClientGeneration> = emptyList(),
    ): GeneratorConfiguration =
        GeneratorConfiguration(
            language = GeneratorName.KOTLIN,
            output =
                GeneratorOutputConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                ),
            models = models,
            schemas = schemas,
            clients = clients,
        )
}
