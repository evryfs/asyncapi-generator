package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMultiFormatMessage
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlan
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
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
        validator.validate(
            generationInput =
                GenerationInput(
                    schemas = mapOf("UserCreated" to Schema(type = "object")),
                    multiFormatSchemas =
                        mapOf(
                            "UserUpdated" to
                                MultiFormatSchema(
                                    schemaFormat = "application/schema+json;version=draft-07",
                                    schema = mapOf("type" to "object"),
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
                "Supported input: AsyncAPI Schema Objects and native JSON Schema Draft 07 schemas.",
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
                            multiFormatSchemas =
                                mapOf(
                                    "UserUpdated" to
                                        MultiFormatSchema(
                                            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
                                            schema = mapOf("type" to "record"),
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
    fun `allows native avro multi format messages for spring kafka client generation`() {
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
    fun `allows native protobuf multi format messages for spring kafka client generation`() {
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

    private fun generationInputWithMultiFormatSchema(
        namespace: String? = null,
    ): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            multiFormatSchemas = mapOf("UserCreated" to nativeAvroSchema(namespace)),
            polymorphicRelationships = emptyMap(),
            channels = emptyList(),
        )

    private fun generationInputWithMultiFormatMessage(schema: MultiFormatSchema): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
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
                                ),
                            ),
                    ),
                ),
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
