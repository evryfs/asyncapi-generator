package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.analyzedMessage
import dev.banking.asyncapi.generator.core.fixtures.nativeMessage
import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SpringKafkaClientChannelWithoutAddress
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SpringKafkaClientChannelWithoutMessages
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SpringKafkaClientContractNameCollision
import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class SpringKafkaClientContractValidatorTest {
    @Test
    fun `allows channels with messages and distinct generated contract names`() {
        SpringKafkaClientContractValidator.validate(
            channels =
                listOf(
                    channel("accountEvents"),
                    channel("customerEvents"),
                ),
            task = task(),
        )
    }

    @Test
    fun `allows a channel with only multi format messages`() {
        SpringKafkaClientContractValidator.validate(
            channels =
                listOf(
                    AnalyzedChannel(
                        channelName = "accountEvents",
                        topic = "account.events",
                        messages =
                            listOf(
                                nativeMessage(
                                    messageId = "accountUpdated",
                                    messageName = "AccountUpdated",
                                    payloadName = "AccountUpdated",
                                    schema =
                                        MultiFormatSchema(
                                            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
                                            schema =
                                                mapOf(
                                                    "type" to "record",
                                                    "name" to "AccountUpdated",
                                                    "fields" to emptyList<Any>(),
                                                ),
                                        ),
                                ),
                            ),
                    ),
                ),
            task = task(),
        )
    }

    @Test
    fun `rejects a channel without an address when producer generation is enabled`() {
        val error =
            assertFailsWith<SpringKafkaClientChannelWithoutAddress> {
                SpringKafkaClientContractValidator.validate(
                    channels =
                        listOf(
                            channel("auditEvents", topic = null),
                            channel("paymentEvents", topic = null),
                        ),
                    task =
                        task(
                            generateProducers = true,
                            generateConsumers = false,
                        ),
                )
            }

        assertEquals(
            """

            Spring Kafka client generation failed for channel 'auditEvents'.
            The AsyncAPI channel has no address.
            Declare channels.auditEvents.address before generating client contracts.
            """.trimIndent(),
            error.message,
        )
    }

    @Test
    fun `rejects a channel without an address when consumer generation is enabled`() {
        assertFailsWith<SpringKafkaClientChannelWithoutAddress> {
            SpringKafkaClientContractValidator.validate(
                channels = listOf(channel("auditEvents", topic = null)),
                task =
                    task(
                        generateProducers = false,
                        generateConsumers = true,
                    ),
            )
        }
    }

    @Test
    fun `rejects a channel without messages`() {
        val error =
            assertFailsWith<SpringKafkaClientChannelWithoutMessages> {
                SpringKafkaClientContractValidator.validate(
                    channels =
                        listOf(
                            AnalyzedChannel(
                                channelName = "auditEvents",
                                topic = "audit.events",
                                messages = emptyList(),
                            ),
                        ),
                    task = task(),
                )
            }

        assertEquals(
            """

            Spring Kafka client generation failed for channel 'auditEvents'.
            The channel does not declare any messages.
            Declare at least one Message Object under channels.auditEvents.messages before generating client contracts.
            """.trimIndent(),
            error.message,
        )
    }

    @Test
    fun `rejects channel IDs that resolve to the same producer and consumer names`() {
        val error =
            assertFailsWith<SpringKafkaClientContractNameCollision> {
                SpringKafkaClientContractValidator.validate(
                    channels =
                        listOf(
                            channel("account-events"),
                            channel("account_events"),
                        ),
                    task = task(),
                )
            }

        assertEquals(
            """

            Spring Kafka client generation failed because channel IDs collide after normalization.
            ['account-events', 'account_events'] resolve to generated contract base name 'AccountEvents'.
            The following contracts would be written more than once: ['AccountEventsProducer', 'AccountEventsConsumer']
            Use channel IDs that remain unique after conversion to PascalCase source-code identifiers.
            """.trimIndent(),
            error.message,
        )
    }

    @Test
    fun `reports only enabled contract names for a channel collision`() {
        val error =
            assertFailsWith<SpringKafkaClientContractNameCollision> {
                SpringKafkaClientContractValidator.validate(
                    channels =
                        listOf(
                            channel("account-events"),
                            channel("account_events"),
                        ),
                    task =
                        task(
                            generateProducers = false,
                            generateConsumers = true,
                        ),
                )
            }

        assertFalse(error.message!!.contains("AccountEventsProducer"))
        assertTrue(error.message!!.contains("AccountEventsConsumer"))
    }

    private fun channel(
        channelName: String,
        topic: String? = "$channelName.v1",
    ): AnalyzedChannel =
        AnalyzedChannel(
            channelName = channelName,
            topic = topic,
            messages =
                listOf(
                    analyzedMessage(
                        messageId = "accountUpdated",
                        messageName = "AccountUpdated",
                        payloadTypeName = "AccountUpdatedPayload",
                        schema = Schema(type = JsonSchemaType.Single("object")),
                    ),
                ),
        )

    private fun task(
        generateProducers: Boolean = true,
        generateConsumers: Boolean = true,
    ): GenerationTask.SpringKafkaClient =
        GenerationTask.SpringKafkaClient(
            language = SourceLanguage.KOTLIN,
            client =
                ClientGeneration(
                    packageName = "com.example.client",
                    modelPackageName = "com.example.model",
                    producer = ClientGeneration.Producer(enabled = generateProducers),
                    consumer = ClientGeneration.Consumer(enabled = generateConsumers),
                ),
        )
}
