package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.SchemaFixtures
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SpringKafkaChannelContractFactoryTest {
    private val factory =
        SpringKafkaChannelContractFactory(
            modelPackage = "com.example.model",
            additionalPayloadTypes =
                linkedSetOf(
                    AdditionalProducerPayloadType.STRING,
                    AdditionalProducerPayloadType.BYTE_ARRAY,
                ),
            topicParameterProperties =
                TopicParameterProperties.fromConfigurationValues(
                    values = mapOf("region" to "kafka.region"),
                    path = "clientConfig.topicParameterProperties",
                ),
        )

    @Test
    fun `prepares channel identity topic keys and method names`() {
        val contract =
            factory.create(
                AnalyzedChannel(
                    channelName = "accountEvents",
                    topic = "accounts.{region}.events",
                    messages =
                        listOf(
                            AnalyzedMessage(
                                messageName = "AccountCreated",
                                payloadTypeName = "AccountCreatedPayload",
                                schema =
                                    Schema(
                                        type = "object",
                                        properties = mapOf("accountId" to SchemaFixtures.inline(type = "string")),
                                    ),
                                keySchema = SchemaFixtures.inline(type = "string"),
                            ),
                            AnalyzedMessage(
                                messageName = "Heartbeat",
                                payloadTypeName = null,
                                schema = null,
                            ),
                        ),
                ),
            )

        assertEquals("AccountEvents", contract.baseName)
        assertEquals("accounts.{region}.events", contract.topic)
        assertEquals("ACCOUNT_EVENTS_TOPIC_ADDRESS", contract.topicAddress.constantName)
        assertEquals("accounts.${'$'}{kafka.region}.events", contract.topicAddress.propertyPlaceholderValue)

        val accountCreated = contract.messages[0]
        assertEquals("listenAccountCreated", accountCreated.consumerMethodName)
        assertEquals("String", accountCreated.keyContract?.javaTypeName)
        assertEquals("String", accountCreated.keyContract?.kotlinTypeName)
        assertFalse(accountCreated.keyContract?.nullable ?: true)
        assertEquals(
            listOf(
                "sendAccountCreated",
                "sendAccountCreatedByteArray",
                "sendAccountCreatedString",
            ),
            accountCreated.producerMethods.map { method -> method.methodName },
        )
        assertNull(accountCreated.producerMethods[0].additionalPayloadType)
        assertEquals(
            AdditionalProducerPayloadType.BYTE_ARRAY,
            accountCreated.producerMethods[1].additionalPayloadType,
        )
        assertEquals(
            AdditionalProducerPayloadType.STRING,
            accountCreated.producerMethods[2].additionalPayloadType,
        )

        val heartbeat = contract.messages[1]
        assertEquals("listenHeartbeat", heartbeat.consumerMethodName)
        assertNull(heartbeat.keyContract)
        assertEquals(listOf("sendHeartbeat"), heartbeat.producerMethods.map { method -> method.methodName })
        assertNull(heartbeat.producerMethods.single().additionalPayloadType)
    }
}
