package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMultiFormatMessage
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.DocumentFormat
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlan
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedSourceSchemaFeature
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GenerationInputCompatibilityValidatorTest {
    private val validator = GenerationInputCompatibilityValidator()

    @Test
    fun `allows asyncapi schema object input for model and avro projection tasks`() {
        validator.validate(
            generationInput =
                GenerationInput(
                    schemas = mapOf("UserCreated" to Schema(type = "object")),
                    polymorphicRelationships = emptyMap(),
                    channels = emptyList(),
                ),
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.ModelArtifacts(
                            language = SourceLanguage.KOTLIN,
                            packageName = "com.example.model",
                        ),
                        GenerationTask.AvroSchemaArtifacts(
                            packageName = "com.example.avro",
                        ),
                    ),
                ),
        )
    }

    @Test
    fun `model artifacts reject tuple form items`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validateSchemaTask(
                    schema = Schema(tupleItems = listOf(inline(Schema(type = "string")))),
                    task = modelTask(),
                )
            }

        assertTrue(error.message!!.contains("Model generation"))
        assertTrue(error.message!!.contains("tuple-form 'items'"))
    }

    @Test
    fun `model artifacts reject false items`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validateSchemaTask(
                    schema = Schema(items = SchemaInterface.BooleanSchema(false)),
                    task = modelTask(),
                )
            }

        assertTrue(error.message!!.contains("'items: false'"))
    }

    @Test
    fun `model artifacts reject an untyped enum containing non string values`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validateSchemaTask(
                    schema = Schema(enum = listOf("OPEN", 1)),
                    task = modelTask(),
                )
            }

        assertTrue(error.message!!.contains("enum without 'type'"))
    }

    @Test
    fun `model artifacts reject a pattern that Java cannot compile`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validateSchemaTask(
                    schema = Schema(type = "string", pattern = "["),
                    task = modelTask(),
                )
            }

        assertTrue(error.message!!.contains("'pattern' that Java cannot compile"))
    }

    @Test
    fun `avro projection rejects tuple form items`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validateSchemaTask(
                    schema = Schema(tupleItems = listOf(inline(Schema(type = "string")))),
                    task = GenerationTask.AvroSchemaArtifacts(packageName = "com.example.avro"),
                )
            }

        assertTrue(error.message!!.contains("Avro projection"))
        assertTrue(error.message!!.contains("tuple-form 'items'"))
    }

    @Test
    fun `avro projection rejects false items`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validateSchemaTask(
                    schema = Schema(items = SchemaInterface.BooleanSchema(false)),
                    task = GenerationTask.AvroSchemaArtifacts(packageName = "com.example.avro"),
                )
            }

        assertTrue(error.message!!.contains("'items: false'"))
    }

    @Test
    fun `avro projection rejects an untyped enum containing non string values`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validateSchemaTask(
                    schema = Schema(enum = listOf("OPEN", 1)),
                    task = GenerationTask.AvroSchemaArtifacts(packageName = "com.example.avro"),
                )
            }

        assertTrue(error.message!!.contains("enum without 'type'"))
    }

    @Test
    fun `avro projection allows a pattern that Java cannot compile`() {
        validateSchemaTask(
            schema = Schema(type = "string", pattern = "["),
            task = GenerationTask.AvroSchemaArtifacts(packageName = "com.example.avro"),
        )
    }

    @Test
    fun `kafka key model artifacts validate selected key models only`() {
        val selectedKey = Schema(type = "object")
        val unrelatedPayload = Schema(tupleItems = listOf(inline(Schema(type = "string"))))

        validator.validate(
            generationInput =
                GenerationInput(
                    schemas =
                        linkedMapOf(
                            "UnrelatedPayload" to unrelatedPayload,
                            "SelectedKey" to selectedKey,
                        ),
                    polymorphicRelationships = emptyMap(),
                    channels =
                        listOf(
                            AnalyzedChannel(
                                channelName = "events",
                                topic = "events",
                                messages =
                                    listOf(
                                        AnalyzedMessage(
                                            messageName = "Event",
                                            payloadTypeName = "UnrelatedPayload",
                                            schema = unrelatedPayload,
                                            keySchema = inline(Schema(type = "object", title = "SelectedKey")),
                                        ),
                                    ),
                            ),
                        ),
                ),
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.KafkaKeyModelArtifacts(
                            language = SourceLanguage.KOTLIN,
                            packageName = "com.example.model",
                        ),
                    ),
                ),
        )
    }

    @Test
    fun `kafka key model artifacts reject an incompatible selected key model`() {
        val selectedKey = Schema(items = SchemaInterface.BooleanSchema(false))
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = mapOf("SelectedKey" to selectedKey),
                            polymorphicRelationships = emptyMap(),
                            channels =
                                listOf(
                                    AnalyzedChannel(
                                        channelName = "events",
                                        topic = "events",
                                        messages =
                                            listOf(
                                                AnalyzedMessage(
                                                    messageName = "Event",
                                                    payloadTypeName = null,
                                                    schema = null,
                                                    keySchema = inline(Schema(type = "object", title = "SelectedKey")),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.KafkaKeyModelArtifacts(
                                    language = SourceLanguage.JAVA,
                                    packageName = "com.example.model",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Kafka key model generation"))
        assertTrue(error.message!!.contains("schema 'SelectedKey'"))
    }

    @Test
    fun `active spring kafka rejects a Java incompatible ordinary message key pattern`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validator.validate(
                    generationInput =
                        inputWithMessage(
                            AnalyzedMessage(
                                messageName = "Event",
                                payloadTypeName = "EventPayload",
                                schema = Schema(type = "object"),
                                keySchema = inline(Schema(type = "string", pattern = "[")),
                            ),
                        ),
                    generationPlan = GenerationPlan(listOf(springKafkaTask())),
                )
            }

        assertTrue(error.message!!.contains("Spring Kafka client generation"))
        assertTrue(error.message!!.contains("schema 'Event'"))
        assertTrue(error.message!!.contains("'pattern' that Java cannot compile"))
    }

    @Test
    fun `active spring kafka allows an object key with a nested Java incompatible pattern`() {
        validator.validate(
            generationInput =
                inputWithMessage(
                    AnalyzedMessage(
                        messageName = "Event",
                        payloadTypeName = "EventPayload",
                        schema = Schema(type = "object"),
                        keySchema =
                            inline(
                                Schema(
                                    type = "object",
                                    properties =
                                        mapOf(
                                            "value" to inline(Schema(type = "string", pattern = "[")),
                                        ),
                                ),
                            ),
                    ),
                ),
            generationPlan = GenerationPlan(listOf(springKafkaTask())),
        )
    }

    @Test
    fun `active spring kafka rejects a Java incompatible multi format message key pattern`() {
        val schema = nativeAvroSchema()
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = emptyMap(),
                            schemaDeclarations = SchemaDeclarationCatalog(multiFormatSchemas = mapOf("Event" to schema)),
                            polymorphicRelationships = emptyMap(),
                            channels =
                                listOf(
                                    AnalyzedChannel(
                                        channelName = "events",
                                        topic = "events",
                                        messages = emptyList(),
                                        multiFormatMessages =
                                            listOf(
                                                AnalyzedMultiFormatMessage(
                                                    messageName = "Event",
                                                    payloadName = "Event",
                                                    schema = schema,
                                                    keySchema = inline(Schema(type = "string", pattern = "[")),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    generationPlan = GenerationPlan(listOf(springKafkaTask())),
                )
            }

        assertTrue(error.message!!.contains("schema 'Event'"))
    }

    @Test
    fun `spring kafka key pattern validation does not inspect payload or header patterns`() {
        validator.validate(
            generationInput =
                inputWithMessage(
                    AnalyzedMessage(
                        messageName = "Event",
                        payloadTypeName = "EventPayload",
                        schema = Schema(type = "string", pattern = "["),
                        keySchema = inline(Schema(type = "string", pattern = "[a-z]+")),
                        headers =
                            AnalyzedMessageHeaders(
                                properties = mapOf("trace" to inline(Schema(type = "string", pattern = "["))),
                            ),
                    ),
                ),
            generationPlan = GenerationPlan(listOf(springKafkaTask())),
        )
    }

    @Test
    fun `disabled spring kafka does not validate key patterns`() {
        validator.validate(
            generationInput =
                inputWithMessage(
                    AnalyzedMessage(
                        messageName = "Event",
                        payloadTypeName = "EventPayload",
                        schema = Schema(type = "object"),
                        keySchema = inline(Schema(type = "string", pattern = "[")),
                    ),
                ),
            generationPlan =
                GenerationPlan(
                    listOf(
                        springKafkaTask(
                            generateProducers = false,
                            generateConsumers = false,
                        ),
                    ),
                ),
        )
    }

    @Test
    fun `document and JSON Schema artifacts allow source incompatible schemas`() {
        val schema =
            Schema(
                tupleItems = listOf(inline(Schema(type = "string"))),
                pattern = "[",
            )
        val input =
            GenerationInput(
                schemas = mapOf("Event" to schema),
                polymorphicRelationships = emptyMap(),
                channels = emptyList(),
            )

        validator.validate(
            generationInput = input,
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.DocumentArtifact(
                            file = File("asyncapi.yaml"),
                            format = DocumentFormat.YAML,
                        ),
                    ),
                ),
        )
        validator.validate(
            generationInput = input,
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.JsonSchemaArtifacts(packageName = "com.example.schema"),
                    ),
                ),
        )
    }

    @Test
    fun `rejects unsupported quarkus kafka generation`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedGenerationCapability> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = emptyMap(),
                            polymorphicRelationships = emptyMap(),
                            channels = emptyList(),
                        ),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.QuarkusKafkaClient(
                                    language = SourceLanguage.KOTLIN,
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Quarkus Kafka client generation is not implemented"))
    }

    @Test
    fun `rejects multi format schemas for model generation`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                validator.validate(
                    generationInput = generationInputWithMultiFormatSchema(),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.ModelArtifacts(
                                    language = SourceLanguage.JAVA,
                                    packageName = "com.example.model",
                                    javaModelType = JavaModelType.RECORD,
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Model generation cannot consume payload 'UserCreated'"))
        assertTrue(error.message!!.contains("application/vnd.apache.avro+json;version=1.9.0"))
    }

    @Test
    fun `rejects multi format schemas for avro projection`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                validator.validate(
                    generationInput = generationInputWithMultiFormatSchema(),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.AvroSchemaArtifacts(
                                    packageName = "com.example.avro",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Avro Projection cannot consume payload 'UserCreated'"))
        assertTrue(error.message!!.contains("This output currently supports AsyncAPI Schema Object payloads only."))
    }

    @Test
    fun `allows native Avro schemas when Avro schema generation includes native artifacts`() {
        validator.validate(
            generationInput = generationInputWithMultiFormatSchema(),
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.AvroSchemaArtifacts(
                            packageName = "com.example.avro",
                        ),
                        GenerationTask.NativeAvroArtifacts(
                            generateSpecificRecords = false,
                            schemaPackageName = "com.example.avro",
                        ),
                    ),
            ),
        )
    }

    @Test
    fun `allows native Protobuf schemas when dedicated generation is planned with Avro projection`() {
        validator.validate(
            generationInput =
                GenerationInput(
                    schemas = mapOf("Account" to Schema(type = "object")),
                    schemaDeclarations =
                        SchemaDeclarationCatalog(
                            multiFormatSchemas = mapOf("UserCreated" to nativeProtobufSchema()),
                        ),
                    polymorphicRelationships = emptyMap(),
                    channels = emptyList(),
                ),
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.AvroSchemaArtifacts(
                            packageName = "com.example.avro",
                        ),
                        GenerationTask.NativeProtobufArtifacts(
                            schemaPackageName = "com.example.protobuf",
                        ),
                    ),
                ),
        )
    }

    @Test
    fun `rejects multi format schemas without their dedicated generation task alongside Avro projection`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = mapOf("Account" to Schema(type = "object")),
                            schemaDeclarations =
                                SchemaDeclarationCatalog(
                                    multiFormatSchemas = mapOf("UserCreated" to nativeProtobufSchema()),
                                ),
                            polymorphicRelationships = emptyMap(),
                            channels = emptyList(),
                        ),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.AvroSchemaArtifacts(
                                    packageName = "com.example.avro",
                                ),
                                GenerationTask.NativeAvroArtifacts(
                                    generateSpecificRecords = false,
                                    schemaPackageName = "com.example.avro",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Avro Projection cannot consume payload 'UserCreated'"))
        assertTrue(error.message!!.contains("application/vnd.google.protobuf;version=3"))
    }

    @Test
    fun `rejects unhandled native format when only one format has a generation task`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = mapOf("Account" to Schema(type = "object")),
                            schemaDeclarations =
                                SchemaDeclarationCatalog(
                                    multiFormatSchemas =
                                        mapOf(
                                            "AvroPayload" to nativeAvroSchema(),
                                            "ProtobufPayload" to nativeProtobufSchema(),
                                        ),
                                ),
                            polymorphicRelationships = emptyMap(),
                            channels = emptyList(),
                        ),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.NativeAvroArtifacts(
                                    generateSpecificRecords = false,
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("ProtobufPayload"))
        assertTrue(error.message!!.contains("application/vnd.google.protobuf;version=3"))
    }

    @Test
    fun `allows mixed formats when both have generation tasks`() {
        validator.validate(
            generationInput =
                GenerationInput(
                    schemas = mapOf("Account" to Schema(type = "object")),
                    schemaDeclarations =
                        SchemaDeclarationCatalog(
                            multiFormatSchemas =
                                mapOf(
                                    "AvroPayload" to nativeAvroSchema(),
                                    "ProtobufPayload" to nativeProtobufSchema(),
                                ),
                        ),
                    polymorphicRelationships = emptyMap(),
                    channels = emptyList(),
                ),
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.NativeAvroArtifacts(
                            generateSpecificRecords = false,
                        ),
                        GenerationTask.NativeProtobufArtifacts(),
                    ),
                ),
        )
    }

    @Test
    fun `allows Boolean-only input for JSON Schema generation`() {
        validator.validate(
            generationInput =
                GenerationInput(
                    schemas = emptyMap(),
                    schemaDeclarations =
                        SchemaDeclarationCatalog(
                            booleanSchemas = mapOf("Allowed" to true),
                        ),
                    polymorphicRelationships = emptyMap(),
                    channels = emptyList(),
                ),
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.JsonSchemaArtifacts(
                            packageName = "com.example.schema",
                        ),
                    ),
                ),
        )
    }

    @Test
    fun `allows native Avro model package matching the schema namespace`() {
        validator.validate(
            generationInput =
                generationInputWithMultiFormatSchema(
                    namespace = "com.example.avro",
                ),
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.NativeAvroArtifacts(
                            generateSpecificRecords = true,
                            modelPackageName = "com.example.avro",
                        ),
                    ),
                ),
        )
    }

    @Test
    fun `rejects native Avro model package differing from the schema namespace`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.NativeAvroModelPackageMismatch> {
                validator.validate(
                    generationInput =
                        generationInputWithMultiFormatSchema(
                            namespace = "com.example.contract",
                        ),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.NativeAvroArtifacts(
                                    generateSpecificRecords = true,
                                    modelPackageName = "com.example.configured",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("payload 'UserCreated'"))
        assertTrue(error.message!!.contains("modelPackage 'com.example.configured'"))
        assertTrue(error.message!!.contains("Avro namespace 'com.example.contract'"))
    }

    @Test
    fun `rejects configured native Avro model package when the schema has no namespace`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.NativeAvroModelPackageMismatch> {
                validator.validate(
                    generationInput = generationInputWithMultiFormatSchema(),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.NativeAvroArtifacts(
                                    generateSpecificRecords = true,
                                    modelPackageName = "com.example.configured",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Avro namespace '<default package>'"))
    }

    @Test
    fun `rejects AsyncAPI Schema Objects for native Protobuf generation`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedSchemaGenerationInput> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = mapOf("UserCreated" to Schema(type = "object")),
                            polymorphicRelationships = emptyMap(),
                            channels = emptyList(),
                        ),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.NativeProtobufArtifacts(
                                    schemaPackageName = "com.example.protobuf",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Native Protobuf generation cannot consume payload 'UserCreated'"))
        assertTrue(error.message!!.contains("Supported input: native Protobuf schemas."))
    }

    @Test
    fun `rejects native Avro schemas for native Protobuf generation`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedSchemaGenerationInput> {
                validator.validate(
                    generationInput = generationInputWithMultiFormatSchema(),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.NativeProtobufArtifacts(
                                    schemaPackageName = "com.example.protobuf",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("application/vnd.apache.avro+json;version=1.9.0"))
        assertTrue(error.message!!.contains("Supported input: native Protobuf schemas."))
    }

    @Test
    fun `allows AsyncAPI and native Draft 07 schemas for JSON Schema generation`() {
        val userCreatedSchema = Schema(type = "object")
        validator.validate(
            generationInput =
                GenerationInput(
                    schemas = mapOf("UserCreated" to userCreatedSchema),
                    schemaDeclarations =
                        SchemaDeclarationCatalog(
                            asyncApiSchemas = mapOf("UserCreated" to userCreatedSchema),
                            multiFormatSchemas =
                                mapOf(
                                    "UserUpdated" to
                                        MultiFormatSchema(
                                            schemaFormat = "application/schema+json;version=draft-07",
                                            schema = mapOf("type" to "object"),
                                        ),
                                ),
                        ),
                    polymorphicRelationships = emptyMap(),
                    channels = emptyList(),
                ),
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.JsonSchemaArtifacts(
                            packageName = "com.example.schema",
                        ),
                    ),
                ),
        )
    }

    @Test
    fun `rejects incompatible native formats for JSON Schema generation`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedSchemaGenerationInput> {
                validator.validate(
                    generationInput = generationInputWithMultiFormatSchema(),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.JsonSchemaArtifacts(
                                    packageName = "com.example.schema",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("JSON Schema generation cannot consume payload 'UserCreated'"))
        assertTrue(
            error.message!!.contains(
                "Supported input: AsyncAPI Schema Objects, Boolean schemas, and native JSON Schema Draft 07 schemas.",
            ),
        )
    }

    @Test
    fun `rejects incompatible native formats when regular JSON schemas are also present`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedSchemaGenerationInput> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = mapOf("UserCreated" to Schema(type = "object")),
                            schemaDeclarations =
                                SchemaDeclarationCatalog(
                                    multiFormatSchemas =
                                        mapOf(
                                            "UserUpdated" to
                                                MultiFormatSchema(
                                                    schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
                                                    schema = mapOf("type" to "record"),
                                                ),
                                        ),
                                ),
                            polymorphicRelationships = emptyMap(),
                            channels = emptyList(),
                        ),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.JsonSchemaArtifacts(
                                    packageName = "com.example.schema",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("JSON Schema generation cannot consume payload 'UserUpdated'"))
    }

    @Test
    fun `rejects empty input for schema generation`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.MissingSchemaGenerationInput> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = emptyMap(),
                            polymorphicRelationships = emptyMap(),
                            channels = emptyList(),
                        ),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.JsonSchemaArtifacts(
                                    packageName = "com.example.schema",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("did not find any compatible schemas"))
    }

    @Test
    fun `allows native Avro declarations used by spring kafka client messages`() {
        validator.validate(
            generationInput = generationInputWithMultiFormatMessage(nativeAvroSchema()),
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.SpringKafkaClient(
                            language = SourceLanguage.KOTLIN,
                            clientPackage = "com.example.kafka",
                            modelPackage = "com.example.model",
                        ),
                    ),
                ),
        )
    }

    @Test
    fun `allows native Protobuf declarations used by spring kafka client messages`() {
        validator.validate(
            generationInput = generationInputWithMultiFormatMessage(nativeProtobufSchema()),
            generationPlan =
                GenerationPlan(
                    listOf(
                        GenerationTask.SpringKafkaClient(
                            language = SourceLanguage.KOTLIN,
                            clientPackage = "com.example.kafka",
                            modelPackage = "com.example.model",
                        ),
                    ),
                ),
        )
    }

    @Test
    fun `rejects unrelated native Protobuf declaration with active spring kafka generation`() {
        val usedAvroSchema = nativeAvroSchema()
        val unrelatedProtobufSchema = nativeProtobufSchema()
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = emptyMap(),
                            schemaDeclarations =
                                SchemaDeclarationCatalog(
                                    multiFormatSchemas =
                                        linkedMapOf(
                                            "UserCreated" to usedAvroSchema,
                                            "AuditRecorded" to unrelatedProtobufSchema,
                                        ),
                                ),
                            polymorphicRelationships = emptyMap(),
                            channels =
                                listOf(
                                    AnalyzedChannel(
                                        channelName = "userEvents",
                                        topic = "users",
                                        messages = emptyList(),
                                        multiFormatMessages =
                                            listOf(
                                                AnalyzedMultiFormatMessage(
                                                    messageName = "UserCreated",
                                                    payloadName = "UserCreated",
                                                    schema = usedAvroSchema,
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    generationPlan = GenerationPlan(listOf(springKafkaTask())),
                )
            }

        assertTrue(error.message!!.contains("Generation cannot consume payload 'AuditRecorded'"))
        assertTrue(error.message!!.contains("application/vnd.google.protobuf;version=3"))
    }

    @Test
    fun `rejects unrelated native Avro declaration with active spring kafka generation`() {
        val usedProtobufSchema = nativeProtobufSchema()
        val unrelatedAvroSchema = nativeAvroSchema()
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = emptyMap(),
                            schemaDeclarations =
                                SchemaDeclarationCatalog(
                                    multiFormatSchemas =
                                        linkedMapOf(
                                            "UserCreated" to usedProtobufSchema,
                                            "AuditRecorded" to unrelatedAvroSchema,
                                        ),
                                ),
                            polymorphicRelationships = emptyMap(),
                            channels =
                                listOf(
                                    AnalyzedChannel(
                                        channelName = "userEvents",
                                        topic = "users",
                                        messages = emptyList(),
                                        multiFormatMessages =
                                            listOf(
                                                AnalyzedMultiFormatMessage(
                                                    messageName = "UserCreated",
                                                    payloadName = "UserCreated",
                                                    schema = usedProtobufSchema,
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    generationPlan = GenerationPlan(listOf(springKafkaTask())),
                )
            }

        assertTrue(error.message!!.contains("Generation cannot consume payload 'AuditRecorded'"))
        assertTrue(error.message!!.contains("application/vnd.apache.avro+json;version=1.9.0"))
    }

    @Test
    fun `disabled spring kafka generation does not handle a native declaration`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                validator.validate(
                    generationInput = generationInputWithMultiFormatMessage(nativeAvroSchema()),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                springKafkaTask(
                                    generateProducers = false,
                                    generateConsumers = false,
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Generation cannot consume payload 'UserCreated'"))
    }

    @Test
    fun `disabled spring kafka generation does not reject Draft 07 message handled by JSON Schema task`() {
        validator.validate(
            generationInput =
                generationInputWithMultiFormatMessage(
                    MultiFormatSchema(
                        schemaFormat = "application/schema+json;version=draft-07",
                        schema = mapOf("type" to "object"),
                    ),
                ),
            generationPlan =
                GenerationPlan(
                    listOf(
                        springKafkaTask(
                            generateProducers = false,
                            generateConsumers = false,
                        ),
                        GenerationTask.JsonSchemaArtifacts(
                            packageName = "com.example.schema",
                        ),
                    ),
                ),
        )
    }

    @Test
    fun `native artifact task handles declaration unrelated to active spring kafka message`() {
        val usedAvroSchema = nativeAvroSchema()
        val unrelatedProtobufSchema = nativeProtobufSchema()
        validator.validate(
            generationInput =
                GenerationInput(
                    schemas = emptyMap(),
                    schemaDeclarations =
                        SchemaDeclarationCatalog(
                            multiFormatSchemas =
                                linkedMapOf(
                                    "UserCreated" to usedAvroSchema,
                                    "AuditRecorded" to unrelatedProtobufSchema,
                                ),
                        ),
                    polymorphicRelationships = emptyMap(),
                    channels =
                        listOf(
                            AnalyzedChannel(
                                channelName = "userEvents",
                                topic = "users",
                                messages = emptyList(),
                                multiFormatMessages =
                                    listOf(
                                        AnalyzedMultiFormatMessage(
                                            messageName = "UserCreated",
                                            payloadName = "UserCreated",
                                            schema = usedAvroSchema,
                                        ),
                                    ),
                            ),
                        ),
                ),
            generationPlan =
                GenerationPlan(
                    listOf(
                        springKafkaTask(),
                        GenerationTask.NativeProtobufArtifacts(),
                    ),
                ),
        )
    }

    @Test
    fun `rejects unsupported multi format messages for spring kafka client generation`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                validator.validate(
                    generationInput =
                        generationInputWithMultiFormatMessage(
                            MultiFormatSchema(
                                schemaFormat = "application/schema+json;version=draft-07",
                                schema = mapOf("type" to "object"),
                            ),
                            keySchema = inline(Schema(type = "string", pattern = "[")),
                        ),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.SpringKafkaClient(
                                    language = SourceLanguage.KOTLIN,
                                    clientPackage = "com.example.kafka",
                                    modelPackage = "com.example.model",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Spring Kafka client generation cannot consume payload 'UserCreated'"))
        assertTrue(error.message!!.contains("Native Avro, Protobuf, and other explicit schema formats"))
    }

    @Test
    fun `rejects channels without messages for spring kafka client generation`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.SpringKafkaClientChannelWithoutMessages> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = emptyMap(),
                            polymorphicRelationships = emptyMap(),
                            channels =
                                listOf(
                                    AnalyzedChannel(
                                        channelName = "auditEvents",
                                        topic = "audit.events",
                                        messages = emptyList(),
                                    ),
                                ),
                        ),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.SpringKafkaClient(
                                    language = SourceLanguage.KOTLIN,
                                    clientPackage = "com.example.kafka",
                                    modelPackage = "com.example.model",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("channel 'auditEvents'"))
        assertTrue(error.message!!.contains("does not declare any messages"))
    }

    @Test
    fun `rejects additional producer method name collisions during compatibility validation`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.SpringKafkaClientMethodNameCollision> {
                validator.validate(
                    generationInput =
                        GenerationInput(
                            schemas = emptyMap(),
                            polymorphicRelationships = emptyMap(),
                            channels =
                                listOf(
                                    AnalyzedChannel(
                                        channelName = "messageEvents",
                                        topic = "message.events",
                                        messages =
                                            listOf(
                                                AnalyzedMessage(
                                                    messageId = "my-message-v1",
                                                    messageName = "MyMessageV1",
                                                    payloadTypeName = "MyMessageV1Payload",
                                                    schema = Schema(type = "object"),
                                                ),
                                                AnalyzedMessage(
                                                    messageId = "my-message-v1-byte-array",
                                                    messageName = "MyMessageV1ByteArray",
                                                    payloadTypeName = "MyMessageV1ByteArrayPayload",
                                                    schema = Schema(type = "object"),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.SpringKafkaClient(
                                    language = SourceLanguage.KOTLIN,
                                    clientPackage = "com.example.kafka",
                                    modelPackage = "com.example.model",
                                    additionalPayloadTypes =
                                        setOf(AdditionalProducerPayloadType.BYTE_ARRAY),
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("generated client method 'sendMyMessageV1ByteArray'"))
    }

    private fun generationInputWithMultiFormatSchema(
        namespace: String? = null,
    ): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            schemaDeclarations =
                SchemaDeclarationCatalog(
                    multiFormatSchemas = mapOf("UserCreated" to nativeAvroSchema(namespace)),
                ),
            polymorphicRelationships = emptyMap(),
            channels = emptyList(),
        )

    private fun validateSchemaTask(
        schema: Schema,
        task: GenerationTask,
    ) {
        validator.validate(
            generationInput =
                GenerationInput(
                    schemas = mapOf("Event" to schema),
                    polymorphicRelationships = emptyMap(),
                    channels = emptyList(),
                ),
            generationPlan = GenerationPlan(listOf(task)),
        )
    }

    private fun modelTask(): GenerationTask.ModelArtifacts =
        GenerationTask.ModelArtifacts(
            language = SourceLanguage.KOTLIN,
            packageName = "com.example.model",
        )

    private fun inputWithMessage(message: AnalyzedMessage): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            polymorphicRelationships = emptyMap(),
            channels =
                listOf(
                    AnalyzedChannel(
                        channelName = "events",
                        topic = "events",
                        messages = listOf(message),
                    ),
                ),
        )

    private fun inline(schema: Schema): SchemaInterface = SchemaInterface.SchemaInline(schema)

    private fun generationInputWithMultiFormatMessage(
        schema: MultiFormatSchema,
        keySchema: SchemaInterface? = null,
    ): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            schemaDeclarations =
                SchemaDeclarationCatalog(
                    multiFormatSchemas = mapOf("UserCreated" to schema),
                ),
            polymorphicRelationships = emptyMap(),
            channels =
                listOf(
                    AnalyzedChannel(
                        channelName = "userEvents",
                        topic = "users",
                        messages = emptyList(),
                        multiFormatMessages =
                            listOf(
                                AnalyzedMultiFormatMessage(
                                    messageName = "UserCreated",
                                    payloadName = "UserCreated",
                                    schema = schema,
                                    keySchema = keySchema,
                                ),
                            ),
                    ),
                ),
        )

    private fun springKafkaTask(
        generateProducers: Boolean = true,
        generateConsumers: Boolean = true,
    ): GenerationTask.SpringKafkaClient =
        GenerationTask.SpringKafkaClient(
            language = SourceLanguage.KOTLIN,
            clientPackage = "com.example.kafka",
            modelPackage = "com.example.model",
            generateProducers = generateProducers,
            generateConsumers = generateConsumers,
        )

    private fun nativeAvroSchema(
        namespace: String? = null,
    ): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
            schema =
                buildMap {
                    put("type", "record")
                    put("name", "UserCreated")
                    namespace?.let { put("namespace", it) }
                    put("fields", emptyList<Any>())
                },
        )

    private fun nativeProtobufSchema(): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.google.protobuf;version=3",
            schema =
                """
                syntax = "proto3";

                package com.example.protobuf;

                option java_multiple_files = true;

                message UserCreated {
                  string user_id = 1;
                }
                """.trimIndent(),
        )
}
