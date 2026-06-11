package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMultiFormatMessage
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlan
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.plan.SpringKafkaClientType
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
                            language = GeneratorName.KOTLIN,
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
                                    language = GeneratorName.JAVA,
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
    fun `rejects multi format messages for spring kafka client generation`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat> {
                validator.validate(
                    generationInput = generationInputWithMultiFormatMessage(),
                    generationPlan =
                        GenerationPlan(
                            listOf(
                                GenerationTask.SpringKafkaClient(
                                    language = GeneratorName.KOTLIN,
                                    clientType = SpringKafkaClientType.SIMPLE,
                                    clientPackage = "com.example.kafka",
                                    modelPackage = "com.example.model",
                                    topicPropertyPrefix = "kafka.topics",
                                ),
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Spring Kafka client generation cannot consume payload 'UserCreated'"))
        assertTrue(error.message!!.contains("Native Avro, Protobuf, and other explicit schema formats"))
    }

    private fun generationInputWithMultiFormatSchema(): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            multiFormatSchemas = mapOf("UserCreated" to nativeAvroSchema()),
            polymorphicRelationships = emptyMap(),
            channels = emptyList(),
        )

    private fun generationInputWithMultiFormatMessage(): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            polymorphicRelationships = emptyMap(),
            channels =
                listOf(
                    AnalyzedChannel(
                        channelName = "userEvents",
                        topic = "users",
                        isProducer = true,
                        isConsumer = true,
                        messages = emptyList(),
                        multiFormatMessages =
                            listOf(
                                AnalyzedMultiFormatMessage(
                                    messageName = "UserCreated",
                                    payloadName = "UserCreated",
                                    schema = nativeAvroSchema(),
                                ),
                            ),
                    ),
                ),
        )

    private fun nativeAvroSchema(): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
            schema = mapOf("type" to "record", "name" to "UserCreated", "fields" to emptyList<Any>()),
        )
}
