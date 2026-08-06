package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SpringKafkaClientMethodNameCollision
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SpringKafkaClientMethodNameValidatorTest {
    @Test
    fun `allows messages with distinct generated method names`() {
        SpringKafkaClientMethodNameValidator.validate(
            channels =
                listOf(
                    channel(
                        message(messageId = "account-created", generatedName = "AccountCreated"),
                        message(messageId = "account-updated", generatedName = "AccountUpdated"),
                    ),
                ),
            task =
                task(
                    additionalPayloadTypes = AdditionalProducerPayloadType.entries.toSet(),
                ),
        )
    }

    @Test
    fun `reports the first generated method for colliding normalized message names`() {
        val error =
            assertFailsWith<SpringKafkaClientMethodNameCollision> {
                SpringKafkaClientMethodNameValidator.validate(
                    channels =
                        listOf(
                            channel(
                                message(messageId = "account-updated", generatedName = "AccountUpdated"),
                                message(messageId = "account_updated", generatedName = "AccountUpdated"),
                            ),
                        ),
                    task = task(),
                )
            }

        assertEquals(
            """

            Spring Kafka client generation failed for channel 'accountEvents'.
            Channel messages ['account-updated', 'account_updated'] resolve to generated client method 'sendAccountUpdated'.
            This method would be generated more than once in the same client contract.
            Give each Message Object a unique 'name', or use unique channel message keys when 'name' is omitted.
            Names must remain unique after conversion to source-code identifiers, including any configured producer method suffixes.
            """.trimIndent(),
            error.message,
        )
    }

    @Test
    fun `rejects a byte array suffix collision with a contract method`() {
        val error =
            assertFailsWith<SpringKafkaClientMethodNameCollision> {
                SpringKafkaClientMethodNameValidator.validate(
                    channels =
                        listOf(
                            channel(
                                message(messageId = "my-message-v1", generatedName = "MyMessageV1"),
                                message(
                                    messageId = "my-message-v1-byte-array",
                                    generatedName = "MyMessageV1ByteArray",
                                ),
                            ),
                        ),
                    task =
                        task(
                            additionalPayloadTypes =
                                setOf(AdditionalProducerPayloadType.BYTE_ARRAY),
                        ),
                )
            }

        assertEquals(
            """

            Spring Kafka client generation failed for channel 'accountEvents'.
            Channel messages ['my-message-v1', 'my-message-v1-byte-array'] resolve to generated client method 'sendMyMessageV1ByteArray'.
            This method would be generated more than once in the same client contract.
            Give each Message Object a unique 'name', or use unique channel message keys when 'name' is omitted.
            Names must remain unique after conversion to source-code identifiers, including any configured producer method suffixes.
            """.trimIndent(),
            error.message,
        )
    }

    @Test
    fun `rejects a string suffix collision with a contract method`() {
        val error =
            assertFailsWith<SpringKafkaClientMethodNameCollision> {
                SpringKafkaClientMethodNameValidator.validate(
                    channels =
                        listOf(
                            channel(
                                message(messageId = "my-message-v1", generatedName = "MyMessageV1"),
                                message(
                                    messageId = "my-message-v1-string",
                                    generatedName = "MyMessageV1String",
                                ),
                            ),
                        ),
                    task =
                        task(
                            additionalPayloadTypes = setOf(AdditionalProducerPayloadType.STRING),
                        ),
                )
            }

        assertTrue(error.message!!.contains("generated client method 'sendMyMessageV1String'"))
        assertTrue(error.message!!.contains("['my-message-v1', 'my-message-v1-string']"))
    }

    @Test
    fun `keeps producer suffix collisions out of consumer contracts`() {
        SpringKafkaClientMethodNameValidator.validate(
            channels =
                listOf(
                    channel(
                        message(messageId = "my-message-v1", generatedName = "MyMessageV1"),
                        message(
                            messageId = "my-message-v1-byte-array",
                            generatedName = "MyMessageV1ByteArray",
                        ),
                    ),
                ),
            task =
                task(
                    additionalPayloadTypes = setOf(AdditionalProducerPayloadType.BYTE_ARRAY),
                    generateProducers = false,
                    generateConsumers = true,
                ),
        )
    }

    @Test
    fun `does not add suffix methods for payloadless messages`() {
        SpringKafkaClientMethodNameValidator.validate(
            channels =
                listOf(
                    channel(
                        message(
                            messageId = "my-message-v1",
                            generatedName = "MyMessageV1",
                            hasPayload = false,
                        ),
                        message(
                            messageId = "my-message-v1-byte-array",
                            generatedName = "MyMessageV1ByteArray",
                        ),
                    ),
                ),
            task =
                task(
                    additionalPayloadTypes = setOf(AdditionalProducerPayloadType.BYTE_ARRAY),
                    generateConsumers = false,
                ),
        )
    }

    private fun channel(vararg messages: AnalyzedMessage): AnalyzedChannel =
        AnalyzedChannel(
            channelName = "accountEvents",
            topic = "account.events",
            messages = messages.toList(),
        )

    private fun message(
        messageId: String,
        generatedName: String,
        hasPayload: Boolean = true,
    ): AnalyzedMessage =
        AnalyzedMessage(
            messageId = messageId,
            messageName = generatedName,
            payloadTypeName = "${generatedName}Payload",
            schema = Schema(type = "object").takeIf { hasPayload },
        )

    private fun task(
        additionalPayloadTypes: Set<AdditionalProducerPayloadType> = emptySet(),
        generateProducers: Boolean = true,
        generateConsumers: Boolean = true,
    ): GenerationTask.SpringKafkaClient =
        GenerationTask.SpringKafkaClient(
            language = SourceLanguage.KOTLIN,
            clientPackage = "com.example.client",
            modelPackage = "com.example.model",
            generateProducers = generateProducers,
            additionalPayloadTypes = additionalPayloadTypes,
            generateConsumers = generateConsumers,
        )
}
