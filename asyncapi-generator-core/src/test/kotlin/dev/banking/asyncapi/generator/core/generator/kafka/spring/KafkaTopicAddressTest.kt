package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KafkaTopicAddressTest {
    private val topicParameterProperties =
        TopicParameterProperties.fromConfigurationValues(
            values =
                mapOf(
                    "DEPLOYMENT_ENVIRONMENT" to "app.kafka.environment",
                    "environment" to "kafka.environment",
                    "tenant-id" to "kafka.tenant-id",
                ),
            path = "clientConfig.topicParameterProperties",
        )

    @Test
    fun `keeps a static topic address unchanged`() {
        val topicAddress =
            KafkaTopicAddress.from(
                channelName = "accountEvents",
                value = "account.events.v1",
                topicParameterProperties = TopicParameterProperties.EMPTY,
            )

        assertEquals("account.events.v1", topicAddress.propertyPlaceholderValue)
        assertEquals("ACCOUNT_EVENTS_TOPIC_ADDRESS", topicAddress.constantName)
    }

    @Test
    fun `maps channel parameters to configured Spring property placeholders`() {
        val topicAddress =
            KafkaTopicAddress.from(
                channelName = "myAccountEvents",
                value = "my.accounts.{DEPLOYMENT_ENVIRONMENT}.{tenant-id}.events.v1",
                topicParameterProperties = topicParameterProperties,
            )

        assertEquals(
            "my.accounts.${'$'}{app.kafka.environment}.${'$'}{kafka.tenant-id}.events.v1",
            topicAddress.propertyPlaceholderValue,
        )
        assertEquals("MY_ACCOUNT_EVENTS_TOPIC_ADDRESS", topicAddress.constantName)
    }

    @Test
    fun `maps every occurrence of a repeated channel parameter`() {
        val topicAddress =
            KafkaTopicAddress.from(
                channelName = "accountEvents",
                value = "{environment}.account.{environment}.v1",
                topicParameterProperties = topicParameterProperties,
            )

        assertEquals(
            "${'$'}{kafka.environment}.account.${'$'}{kafka.environment}.v1",
            topicAddress.propertyPlaceholderValue,
        )
    }

    @Test
    fun `ignores configured mappings not used by the channel`() {
        val topicAddress =
            KafkaTopicAddress.from(
                channelName = "accountEvents",
                value = "account.events.v1",
                topicParameterProperties = topicParameterProperties,
            )

        assertEquals("account.events.v1", topicAddress.propertyPlaceholderValue)
    }

    @Test
    fun `rejects a channel parameter without an exact mapping`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                KafkaTopicAddress.from(
                    channelName = "accountEvents",
                    value = "account.{environment}.events.v1",
                    topicParameterProperties =
                        TopicParameterProperties.fromConfigurationValues(
                            values = mapOf("Environment" to "kafka.environment"),
                            path = "clientConfig.topicParameterProperties",
                        ),
                )
            }

        assertEquals(
            "Cannot generate Spring Kafka client for channel 'accountEvents': " +
                "topic address 'account.{environment}.events.v1' uses channel parameters [environment] without " +
                "matching topicParameterProperties entries. Configured entries: [Environment]",
            exception.message,
        )
    }
}
