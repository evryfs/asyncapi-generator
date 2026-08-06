package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.DocumentFormat
import dev.banking.asyncapi.generator.core.generator.configuration.DocumentOutput
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorOutputConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorProfile
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaType
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
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
    fun `generate writes JSON Schema artifacts to resource output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = bundledDocument(),
            generatorConfiguration =
                GeneratorConfiguration(
                    profile = GeneratorProfile.Schema(SchemaType.JSON_SCHEMA),
                    output =
                        GeneratorOutputConfiguration(
                            sourceOutputDirectory = sourceOutputDirectory,
                            javaSourceOutputDirectory = sourceOutputDirectory,
                            resourceOutputDirectory = resourceOutputDirectory,
                        ),
                    schemas = listOf(SchemaGeneration.JsonSchema(packageName = "com.example.jsonschema")),
                ),
        )

        val schemaArtifact = resourceOutputDirectory.resolve("com/example/jsonschema/Task.schema.json")
        assertTrue(schemaArtifact.exists())
        assertTrue(schemaArtifact.readText().contains("\"type\" : \"object\""))
        assertTrue(schemaArtifact.readText().contains("\"${'$'}schema\" : \"http://json-schema.org/draft-07/schema#\""))
        assertFalse(sourceOutputDirectory.resolve("com/example/jsonschema/Task.schema.json").exists())
    }

    @Test
    fun `generate writes bundled AsyncAPI document as YAML`() {
        val outputFile = tempDir.resolve("bundled/asyncapi.yaml").toFile()

        generator.generate(
            asyncApiDocument = bundledDocument(),
            generatorConfiguration =
                documentGeneratorConfiguration(
                    outputFile = outputFile,
                    format = DocumentFormat.YAML,
                ),
        )

        assertTrue(outputFile.exists())
        assertTrue(outputFile.readText().startsWith("asyncapi:"))
        assertTrue(outputFile.readText().contains("components:"))
    }

    @Test
    fun `generate writes bundled AsyncAPI document as JSON`() {
        val outputFile = tempDir.resolve("bundled/asyncapi.json").toFile()

        generator.generate(
            asyncApiDocument = bundledDocument(),
            generatorConfiguration =
                documentGeneratorConfiguration(
                    outputFile = outputFile,
                    format = DocumentFormat.JSON,
                ),
        )

        assertTrue(outputFile.exists())
        assertTrue(outputFile.readText().startsWith("{"))
        assertTrue(outputFile.readText().contains("\"asyncapi\""))
        assertTrue(outputFile.readText().contains("\"components\""))
    }

    @Test
    fun `generate completes artifact rendering before writing any output`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val documentOutputFile = tempDir.resolve("bundled/asyncapi.yaml").toFile()
        val sourceConfiguration =
            generatorConfiguration(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
                models = ModelGeneration.Enabled(packageName = "com.example.model"),
                clients =
                    listOf(
                        ClientGeneration.Kafka(
                            packageName = "com.example.kafka",
                            modelPackageName = "com.example.model",
                            springKafka = ClientGeneration.SpringKafka(),
                        ),
                    ),
            )
        val configuration =
            sourceConfiguration.copy(
                output =
                    sourceConfiguration.output.copy(
                        document =
                            DocumentOutput(
                                file = documentOutputFile,
                                format = DocumentFormat.YAML,
                            ),
                    ),
            )

        val error =
            assertFailsWith<IllegalArgumentException> {
                generator.generate(
                    asyncApiDocument =
                        bundlerFixtures.bundledDocument(
                            File("src/test/resources/generator/spring-kafka/single-message.yaml"),
                        ),
                    generatorConfiguration = configuration,
                )
            }

        assertTrue(error.message!!.contains("without matching topicParameterProperties entries"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
        assertFalse(documentOutputFile.exists())
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
    fun `generate rejects mismatched native Avro model package before writing artifacts`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        val error =
            assertFailsWith<AsyncApiGeneratorException.NativeAvroModelPackageMismatch> {
                generator.generate(
                    asyncApiDocument = generationInputFixtures.documentWithMultiFormatComponent(),
                    generatorConfiguration =
                        generatorConfiguration(
                            sourceOutputDirectory = sourceOutputDirectory,
                            javaSourceOutputDirectory = javaSourceOutputDirectory,
                            resourceOutputDirectory = resourceOutputDirectory,
                            schemas =
                                listOf(
                                    SchemaGeneration.NativeAvro(
                                        generateSpecificRecords = true,
                                        modelPackageName = "com.example.configured",
                                    ),
                                ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("modelPackage 'com.example.configured'"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(javaSourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
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
                    schemas = listOf(SchemaGeneration.NativeProtobuf()),
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
                    schemas =
                        listOf(
                            SchemaGeneration.NativeProtobuf(
                                models = ProtobufModelGeneration(packageName = "com.example.protobuf"),
                            ),
                        ),
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
                    schemas = listOf(SchemaGeneration.NativeProtobuf()),
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

        assertTrue(sourceOutputDirectory.resolve("com/example/kafka/producer/UserEventsProducer.kt").exists())
        assertTrue(sourceOutputDirectory.resolve("com/example/kafka/consumer/UserEventsConsumer.kt").exists())
    }

    @Test
    fun `generate writes object Kafka key models alongside native Avro payload models and clients`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = generationInputFixtures.documentWithNativeAvroMessageAndObjectKey(),
            generatorConfiguration =
                generatorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    javaSourceOutputDirectory = javaSourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    schemas = listOf(SchemaGeneration.NativeAvro(generateSpecificRecords = true)),
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

        val keyModel = sourceOutputDirectory.resolve("com/example/model/UserCreatedKey.kt")
        val producer = sourceOutputDirectory.resolve("com/example/kafka/producer/UserEventsProducer.kt")
        assertTrue(keyModel.exists())
        assertTrue(keyModel.readText().contains("data class UserCreatedKey("))
        assertTrue(producer.exists())
        assertTrue(producer.readText().contains("import com.example.model.UserCreatedKey"))
        assertTrue(javaSourceOutputDirectory.resolve("com/example/avro/UserCreated.java").exists())
        assertTrue(resourceOutputDirectory.resolve("com/example/avro/UserCreated.avsc").exists())
    }

    @Test
    fun `generate writes object Kafka key models alongside native Protobuf payload models and clients`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val protobufModels =
            ProtobufModelGeneration(
                packageName = "com.example.protobuf",
            )

        generator.generate(
            asyncApiDocument = generationInputFixtures.documentWithNativeProtobufMessageAndObjectKey(),
            generatorConfiguration =
                generatorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    javaSourceOutputDirectory = javaSourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    schemas = listOf(SchemaGeneration.NativeProtobuf(models = protobufModels)),
                    clients =
                        listOf(
                            ClientGeneration.Kafka(
                                packageName = "com.example.kafka",
                                modelPackageName = "com.example.protobuf",
                                springKafka = ClientGeneration.SpringKafka(),
                            ),
                        ),
                ),
        )

        val keyModel = sourceOutputDirectory.resolve("com/example/protobuf/UserCreatedKey.kt")
        val consumer = sourceOutputDirectory.resolve("com/example/kafka/consumer/UserEventsConsumer.kt")
        assertTrue(keyModel.exists())
        assertTrue(keyModel.readText().contains("data class UserCreatedKey("))
        assertTrue(consumer.exists())
        assertTrue(consumer.readText().contains("import com.example.protobuf.UserCreatedKey"))
        assertTrue(javaSourceOutputDirectory.resolve("com/example/protobuf/UserCreatedPayload.java").exists())
        assertTrue(resourceOutputDirectory.resolve("com/example/protobuf/UserCreatedPayload.proto").exists())
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
            profile = GeneratorProfile.Source(SourceLanguage.KOTLIN),
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

    private fun documentGeneratorConfiguration(
        outputFile: File,
        format: DocumentFormat,
    ): GeneratorConfiguration =
        GeneratorConfiguration(
            profile = GeneratorProfile.Document(format),
            output =
                GeneratorOutputConfiguration(
                    sourceOutputDirectory = tempDir.resolve("sources").toFile(),
                    resourceOutputDirectory = tempDir.resolve("resources").toFile(),
                    document =
                        DocumentOutput(
                            file = outputFile,
                            format = format,
                        ),
                ),
        )
}
