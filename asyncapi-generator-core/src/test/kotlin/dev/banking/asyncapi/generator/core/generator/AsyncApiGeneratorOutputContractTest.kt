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
    fun `generate writes native Avro schema and SpecificRecord artifacts to output directories`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = generationInputFixtures.documentWithMultiFormatComponent(),
            generatorConfiguration =
                generatorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    javaSourceOutputDirectory = javaSourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    schemas = listOf(SchemaGeneration.NativeAvro(generateSpecificRecords = true)),
                ),
        )

        assertTrue(resourceOutputDirectory.resolve("UserCreated.avsc").exists())
        assertTrue(javaSourceOutputDirectory.resolve("UserCreated.java").exists())
        assertFalse(sourceOutputDirectory.resolve("UserCreated.avsc").exists())
        assertFalse(sourceOutputDirectory.resolve("UserCreated.java").exists())
        assertFalse(resourceOutputDirectory.resolve("UserCreated.java").exists())
    }

    @Test
    fun `generate writes external native Avro schema asset content to output directories`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = externalNativeSchemaAssetsDocument(),
            generatorConfiguration =
                generatorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    javaSourceOutputDirectory = javaSourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    schemas = listOf(SchemaGeneration.NativeAvro(generateSpecificRecords = true)),
                ),
        )

        val schemaArtifact = resourceOutputDirectory.resolve("com/example/external/avro/UserCreatedAvro.avsc")
        val specificRecordArtifact = javaSourceOutputDirectory.resolve("com/example/external/avro/UserCreatedAvro.java")
        assertTrue(schemaArtifact.exists())
        assertTrue(schemaArtifact.readText().contains("\"namespace\" : \"com.example.external.avro\""))
        assertTrue(specificRecordArtifact.exists())
        assertTrue(specificRecordArtifact.readText().contains("package com.example.external.avro;"))
    }

    @Test
    fun `generate writes native Protobuf schema artifacts to resource output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = generationInputFixtures.documentWithNativeProtobufComponent(),
            generatorConfiguration =
                generatorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    javaSourceOutputDirectory = javaSourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    schemas = listOf(SchemaGeneration.NativeProtobuf(generateJavaMessageTypes = false)),
                ),
        )

        assertTrue(resourceOutputDirectory.resolve("com/example/protobuf/UserCreated.proto").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/protobuf/UserCreated.proto").exists())
        assertFalse(javaSourceOutputDirectory.resolve("com/example/protobuf/UserCreated.proto").exists())
    }

    @Test
    fun `generate writes native Protobuf Java message artifacts to Java source output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = generationInputFixtures.documentWithNativeProtobufJavaMessageComponent(),
            generatorConfiguration =
                generatorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    javaSourceOutputDirectory = javaSourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    schemas = listOf(SchemaGeneration.NativeProtobuf()),
                ),
        )

        assertTrue(resourceOutputDirectory.resolve("com/example/protobuf/UserCreated.proto").exists())
        assertTrue(javaSourceOutputDirectory.resolve("com/example/protobuf/UserCreated.java").exists())
        assertTrue(javaSourceOutputDirectory.resolve("com/example/protobuf/UserCreatedOrBuilder.java").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/protobuf/UserCreated.java").exists())
    }

    @Test
    fun `generate writes external native Protobuf schema asset content to resource output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = externalNativeSchemaAssetsDocument(),
            generatorConfiguration =
                generatorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    javaSourceOutputDirectory = javaSourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    schemas = listOf(SchemaGeneration.NativeProtobuf(generateJavaMessageTypes = false)),
                ),
        )

        val schemaArtifact = resourceOutputDirectory.resolve("com/example/external/protobuf/UserCreatedProtobuf.proto")
        assertTrue(schemaArtifact.exists())
        assertTrue(schemaArtifact.readText().contains("package com.example.external.protobuf;"))
        assertTrue(schemaArtifact.readText().contains("message UserCreatedProtobuf"))
        assertFalse(sourceOutputDirectory.resolve("com/example/external/protobuf/UserCreatedProtobuf.proto").exists())
        assertFalse(javaSourceOutputDirectory.resolve("com/example/external/protobuf/UserCreatedProtobuf.proto").exists())
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
    fun `generate writes spring kafka artifacts for native avro message payloads`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = generationInputFixtures.documentWithMultiFormatMessagePayload(),
            generatorConfiguration =
                generatorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    clients =
                        listOf(
                            ClientGeneration.Kafka(
                                packageName = "com.example.kafka",
                                modelPackageName = "com.example.model",
                                springKafka = ClientGeneration.SpringKafka(),
                            ),
                        ),
                ),
        )

        assertTrue(sourceOutputDirectory.resolve("com/example/kafka/producer/UserEventsProducerUserCreated.kt").exists())
        assertTrue(sourceOutputDirectory.resolve("com/example/kafka/consumer/UserEventsConsumer.kt").exists())
    }

    private fun bundledDocument() =
        bundlerFixtures.bundledDocument(
            File("src/test/resources/generator/asyncapi_enum_default_value.yaml"),
        )

    private fun externalNativeSchemaAssetsDocument() =
        bundlerFixtures.bundledDocument(
            File("src/test/resources/generator/native-assets/asyncapi_external_native_schema_assets.yaml"),
        )

    private fun generatorConfiguration(
        sourceOutputDirectory: File,
        resourceOutputDirectory: File,
        javaSourceOutputDirectory: File = sourceOutputDirectory,
        models: ModelGeneration = ModelGeneration.Disabled,
        schemas: List<SchemaGeneration> = emptyList(),
        clients: List<ClientGeneration> = emptyList(),
    ): GeneratorConfiguration =
        GeneratorConfiguration(
            language = GeneratorName.KOTLIN,
            output =
                GeneratorOutputConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    javaSourceOutputDirectory = javaSourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                ),
            models = models,
            schemas = schemas,
            clients = clients,
        )
}
