package dev.banking.asyncapi.generator.core.generator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.fixtures.TestResources
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
import dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedSourceSchemaFeature
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncApiGeneratorOutputContractTest {
    private val jsonMapper = ObjectMapper()
    private val yamlMapper = ObjectMapper(YAMLFactory())
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
                jsonSchemaGeneratorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                ),
        )

        val schemaArtifact = resourceOutputDirectory.resolve("com/example/jsonschema/Task.schema.json")
        assertTrue(schemaArtifact.exists())
        assertTrue(schemaArtifact.readText().contains("\"type\" : \"object\""))
        assertTrue(schemaArtifact.readText().contains("\"${'$'}schema\" : \"http://json-schema.org/draft-07/schema#\""))
        assertFalse(sourceOutputDirectory.resolve("com/example/jsonschema/Task.schema.json").exists())
    }

    @Test
    fun `generate preserves source incompatible constructs in JSON Schema artifacts`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = sourceIncompatibleSchemaDocument(),
            generatorConfiguration =
                jsonSchemaGeneratorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                ),
        )

        val outputDirectory = resourceOutputDirectory.resolve("com/example/jsonschema")
        val tupleItems = jsonMapper.readTree(outputDirectory.resolve("TupleItems.schema.json"))
        val falseItems = jsonMapper.readTree(outputDirectory.resolve("FalseItems.schema.json"))
        val untypedEnum = jsonMapper.readTree(outputDirectory.resolve("UntypedEnum.schema.json"))
        val ecmaPattern = jsonMapper.readTree(outputDirectory.resolve("EcmaPattern.schema.json"))
        val expectedEnum = jsonMapper.valueToTree<JsonNode>(listOf("open", 2, true, null))

        assertEquals(2, tupleItems.path("items").size())
        assertEquals("string", tupleItems.path("items").path(0).path("type").textValue())
        assertEquals("integer", tupleItems.path("items").path(1).path("type").textValue())
        assertEquals(false, falseItems.path("items").booleanValue())
        assertEquals(expectedEnum, untypedEnum.path("enum"))
        assertEquals("(?<group_name>a)", ecmaPattern.path("pattern").textValue())
        assertFalse(sourceOutputDirectory.exists())
    }

    @Test
    fun `generate writes inline message payload as a standalone JSON Schema artifact`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = documentWithInlineMessagePayload(),
            generatorConfiguration =
                jsonSchemaGeneratorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                ),
        )

        val schemaArtifact =
            resourceOutputDirectory.resolve("com/example/jsonschema/AccountUpdatedPayload.schema.json")
        assertTrue(schemaArtifact.exists())
        assertTrue(schemaArtifact.readText().contains("\"type\" : \"object\""))
        assertFalse(sourceOutputDirectory.resolve("com/example/jsonschema/AccountUpdatedPayload.schema.json").exists())
    }

    @Test
    fun `generate names resolved external payload artifact from the final reference fragment`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = resolvedExternalMessagePayloadDocument(),
            generatorConfiguration =
                jsonSchemaGeneratorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                ),
        )

        val schemaArtifact =
            resourceOutputDirectory.resolve("com/example/jsonschema/MyAccountCreatedV1Payload.schema.json")
        assertTrue(schemaArtifact.exists())
        assertTrue(schemaArtifact.readText().contains("\"type\" : \"object\""))
        assertFalse(sourceOutputDirectory.resolve("com/example/jsonschema/MyAccountCreatedV1Payload.schema.json").exists())
    }

    @Test
    fun `generate writes component Boolean schemas as exact scalar artifacts`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        generator.generate(
            asyncApiDocument = documentWithBooleanComponents(),
            generatorConfiguration =
                jsonSchemaGeneratorConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                ),
        )

        val allowedArtifact = resourceOutputDirectory.resolve("com/example/jsonschema/AllowAnything.schema.json")
        val deniedArtifact = resourceOutputDirectory.resolve("com/example/jsonschema/DenyAnything.schema.json")
        assertEquals("true${System.lineSeparator()}", allowedArtifact.readText())
        assertEquals("false${System.lineSeparator()}", deniedArtifact.readText())
        assertFalse(sourceOutputDirectory.resolve("com/example/jsonschema/AllowAnything.schema.json").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/jsonschema/DenyAnything.schema.json").exists())
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
    fun `generate preserves source incompatible constructs in bundled YAML`() {
        val outputFile = tempDir.resolve("bundled/source-incompatible.yaml").toFile()

        generator.generate(
            asyncApiDocument = sourceIncompatibleSchemaDocument(),
            generatorConfiguration =
                documentGeneratorConfiguration(
                    outputFile = outputFile,
                    format = DocumentFormat.YAML,
                ),
        )

        val schemas = yamlMapper.readTree(outputFile).path("components").path("schemas")
        val expectedEnum = jsonMapper.valueToTree<JsonNode>(listOf("open", 2, true, null))

        assertEquals(2, schemas.path("TupleItems").path("items").size())
        assertEquals("string", schemas.path("TupleItems").path("items").path(0).path("type").textValue())
        assertEquals("integer", schemas.path("TupleItems").path("items").path(1).path("type").textValue())
        assertEquals(false, schemas.path("FalseItems").path("items").booleanValue())
        assertEquals(expectedEnum, schemas.path("UntypedEnum").path("enum"))
        assertEquals("(?<group_name>a)", schemas.path("EcmaPattern").path("pattern").textValue())
    }

    @Test
    fun `generate rejects source incompatible models before creating output directories`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                generator.generate(
                    asyncApiDocument = sourceIncompatibleSchemaDocument(),
                    generatorConfiguration =
                        generatorConfiguration(
                            sourceOutputDirectory = sourceOutputDirectory,
                            resourceOutputDirectory = resourceOutputDirectory,
                            models = ModelGeneration.Enabled(packageName = "com.example.model"),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Model generation cannot represent schema 'TupleItems'"))
        assertTrue(error.message!!.contains("tuple-form 'items'"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }

    @Test
    fun `generate writes bundled document containing native Avro without translating generation input`() {
        val outputFile = tempDir.resolve("bundled/native-avro.yaml").toFile()

        generator.generate(
            asyncApiDocument = generationInputFixtures.documentWithMultiFormatComponent(),
            generatorConfiguration =
                documentGeneratorConfiguration(
                    outputFile = outputFile,
                    format = DocumentFormat.YAML,
                ),
        )

        assertTrue(outputFile.exists())
        assertTrue(outputFile.readText().contains("application/vnd.apache.avro+json;version=1.9.0"))
        assertTrue(outputFile.readText().contains("UserCreated"))
    }

    @Test
    fun `generate rejects model-only plan when scalar schema produces no artifacts`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        val error =
            assertFailsWith<AsyncApiGeneratorException.NoArtifactsGenerated> {
                generator.generate(
                    asyncApiDocument = documentWithScalarSchema(),
                    generatorConfiguration =
                        generatorConfiguration(
                            sourceOutputDirectory = sourceOutputDirectory,
                            resourceOutputDirectory = resourceOutputDirectory,
                            models = ModelGeneration.Enabled(packageName = "com.example.model"),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Generation completed without producing any artifacts"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }

    @Test
    fun `generate rejects spring kafka client plan when document has no channels`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        val error =
            assertFailsWith<AsyncApiGeneratorException.NoArtifactsGenerated> {
                generator.generate(
                    asyncApiDocument = documentWithoutChannels(),
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
            }

        assertTrue(error.message!!.contains("Generation completed without producing any artifacts"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }

    @Test
    fun `generate rejects spring kafka channel without address before creating output directories`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()

        val error =
            assertFailsWith<AsyncApiGeneratorException.SpringKafkaClientChannelWithoutAddress> {
                generator.generate(
                    asyncApiDocument = documentWithInlineMessagePayload(),
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
            }

        assertTrue(error.message!!.contains("Declare channels.accountEvents.address"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }

    @Test
    fun `generate allows empty model task when bundled document produces an artifact`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val documentOutputFile = tempDir.resolve("bundled/asyncapi.yaml").toFile()
        val sourceConfiguration =
            generatorConfiguration(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
                models = ModelGeneration.Enabled(packageName = "com.example.model"),
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

        generator.generate(
            asyncApiDocument = documentWithScalarSchema(),
            generatorConfiguration = configuration,
        )

        assertTrue(documentOutputFile.exists())
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }

    @Test
    fun `late native Avro rendering failure leaves earlier outputs unwritten`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val documentOutputFile = tempDir.resolve("bundled/asyncapi.yaml").toFile()
        val sourceConfiguration =
            generatorConfiguration(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
                schemas =
                    listOf(
                        SchemaGeneration.AvroProjection(packageName = "com.example.avro"),
                        SchemaGeneration.NativeAvro(generateSpecificRecords = false),
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
            assertFailsWith<AsyncApiGeneratorException.InvalidNativeAvroSchema> {
                generator.generate(
                    asyncApiDocument = documentWithAsyncApiAndInvalidNativeAvroSchemas(),
                    generatorConfiguration = configuration,
                )
            }

        assertTrue(error.message!!.contains("Native Avro generation failed for payload 'InvalidNative'"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
        assertFalse(documentOutputFile.exists())
    }

    @Test
    fun `schema name collision during input loading creates no output directories`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val document =
            AsyncApiDocument(
                asyncapi = "3.0.0",
                info = Info(title = "Schema collision", version = "1.0.0"),
                components =
                    ComponentInterface.ComponentInline(
                        Component(
                            schemas =
                                mapOf(
                                    "account-key" to SchemaInterface.SchemaInline(Schema(type = "object")),
                                ),
                            messages =
                                mapOf(
                                    "account" to
                                        MessageInterface.MessageInline(
                                            Message(
                                                name = "Account",
                                                bindings =
                                                    mapOf(
                                                        "kafka" to
                                                            BindingInterface.BindingInline(
                                                                Binding(
                                                                    content = emptyMap(),
                                                                    kafkaKeySchema =
                                                                        SchemaInterface.SchemaInline(
                                                                            Schema(type = "object"),
                                                                        ),
                                                                ),
                                                            ),
                                                    ),
                                            ),
                                        ),
                                ),
                        ),
                    ),
            )

        val error =
            assertFailsWith<AsyncApiGeneratorException.SchemaNameCollision> {
                generator.generate(
                    asyncApiDocument = document,
                    generatorConfiguration =
                        generatorConfiguration(
                            sourceOutputDirectory = sourceOutputDirectory,
                            resourceOutputDirectory = resourceOutputDirectory,
                            models = ModelGeneration.Enabled(packageName = "com.example.model"),
                        ),
                )
            }

        assertTrue(error.message!!.contains("AccountKey"))
        assertTrue(error.message!!.contains("components.schemas['account-key']"))
        assertTrue(error.message!!.contains("components.messages['account'].bindings.kafka.key"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }

    @Test
    fun `generate rejects bundled document and generated artifact destination collisions`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val documentOutputFile = sourceOutputDirectory.resolve("com/example/model/Task.kt")
        val sourceConfiguration =
            generatorConfiguration(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
                models = ModelGeneration.Enabled(packageName = "com.example.model"),
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
            assertFailsWith<AsyncApiGeneratorException.GeneratedArtifactCollision> {
                generator.generate(
                    asyncApiDocument = bundledDocument(),
                    generatorConfiguration = configuration,
                )
            }

        assertTrue(error.message!!.contains("SOURCE: com/example/model/Task.kt"))
        assertTrue(error.message!!.contains("BUNDLED_DOCUMENT: ${documentOutputFile.path}"))
        assertFalse(documentOutputFile.exists())
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
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
            asyncApiDocument = externalNativeAvroSchemaAssetsDocument(),
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
            asyncApiDocument = externalNativeProtobufSchemaAssetsDocument(),
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

    private fun sourceIncompatibleSchemaDocument(): AsyncApiDocument =
        AsyncApiDocumentLoader()
            .load(TestResources.file("generator/source-incompatible-schema-features.yaml"))
            .document

    private fun externalNativeAvroSchemaAssetsDocument() =
        bundlerFixtures.bundledDocument(
            File("src/test/resources/generator/native-assets/asyncapi_external_native_avro_schema_assets.yaml"),
        )

    private fun externalNativeProtobufSchemaAssetsDocument() =
        bundlerFixtures.bundledDocument(
            File("src/test/resources/generator/native-assets/asyncapi_external_native_protobuf_schema_assets.yaml"),
        )

    private fun documentWithAsyncApiAndInvalidNativeAvroSchemas(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Render order", version = "1.0.0"),
            components =
                ComponentInterface.ComponentInline(
                    Component(
                        schemas =
                            linkedMapOf(
                                "Task" to
                                    SchemaInterface.SchemaInline(
                                        Schema(
                                            type = "object",
                                            properties =
                                                mapOf(
                                                    "id" to SchemaInterface.SchemaInline(Schema(type = "string")),
                                                ),
                                        ),
                                    ),
                                "InvalidNative" to
                                    SchemaInterface.MultiFormatSchemaInline(
                                        MultiFormatSchema(
                                            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
                                            schema =
                                                mapOf(
                                                    "type" to "record",
                                                    "name" to "InvalidNative",
                                                    "fields" to "not-a-field-list",
                                                ),
                                        ),
                                    ),
                            ),
                    ),
                ),
        )

    private fun documentWithScalarSchema(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Scalar schema", version = "1.0.0"),
            components =
                ComponentInterface.ComponentInline(
                    Component(
                        schemas =
                            mapOf(
                                "Status" to SchemaInterface.SchemaInline(Schema(type = "string")),
                            ),
                    ),
                ),
        )

    private fun documentWithoutChannels(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "No channels", version = "1.0.0"),
        )

    private fun documentWithInlineMessagePayload(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Inline payload", version = "1.0.0"),
            channels =
                mapOf(
                    "accountEvents" to
                        ChannelInterface.ChannelInline(
                            Channel(
                                messages =
                                    mapOf(
                                        "accountUpdated" to
                                            MessageInterface.MessageInline(
                                                Message(
                                                    name = "AccountUpdated",
                                                    payload =
                                                        SchemaInterface.SchemaInline(
                                                            Schema(type = "object"),
                                                        ),
                                                ),
                                            ),
                                    ),
                            ),
                        ),
                ),
        )

    private fun resolvedExternalMessagePayloadDocument(): AsyncApiDocument =
        bundlerFixtures.bundledDocument(
            File("src/test/resources/generator/external-message-payload/main.yaml"),
        )

    private fun documentWithBooleanComponents(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Boolean schemas", version = "1.0.0"),
            components =
                ComponentInterface.ComponentInline(
                    Component(
                        schemas =
                            linkedMapOf(
                                "AllowAnything" to SchemaInterface.BooleanSchema(true),
                                "DenyAnything" to SchemaInterface.BooleanSchema(false),
                            ),
                    ),
                ),
        )

    private fun jsonSchemaGeneratorConfiguration(
        sourceOutputDirectory: File,
        resourceOutputDirectory: File,
    ): GeneratorConfiguration =
        GeneratorConfiguration(
            profile = GeneratorProfile.Schema(SchemaType.JSON_SCHEMA),
            output =
                GeneratorOutputConfiguration(
                    sourceOutputDirectory = sourceOutputDirectory,
                    javaSourceOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                ),
            schemas = listOf(SchemaGeneration.JsonSchema(packageName = "com.example.jsonschema")),
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
