package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TopicParameterPropertiesTest {
    @Test
    fun `keeps configured mappings in deterministic order`() {
        val properties =
            TopicParameterProperties.fromConfigurationValues(
                values =
                    mapOf(
                        "tenant" to "kafka.tenant",
                        "environment" to "kafka.environment",
                    ),
                path = CONFIGURATION_PATH,
            )

        assertEquals(listOf("environment", "tenant"), properties.mappings.keys.toList())
        assertEquals("kafka.environment", properties["environment"])
    }

    @Test
    fun `returns the shared empty configuration for no mappings`() {
        assertEquals(
            TopicParameterProperties.EMPTY,
            TopicParameterProperties.fromConfigurationValues(
                values = emptyMap(),
                path = CONFIGURATION_PATH,
            ),
        )
    }

    @Test
    fun `rejects blank parameter names and property names`() {
        val blankParameter =
            assertFailsWith<IllegalArgumentException> {
                TopicParameterProperties.fromConfigurationValues(
                    values = mapOf(" " to "kafka.environment"),
                    path = CONFIGURATION_PATH,
                )
            }
        val blankProperty =
            assertFailsWith<IllegalArgumentException> {
                TopicParameterProperties.fromConfigurationValues(
                    values = mapOf("environment" to " "),
                    path = CONFIGURATION_PATH,
                )
            }

        assertEquals("$CONFIGURATION_PATH cannot contain an empty parameter name", blankParameter.message)
        assertEquals("$CONFIGURATION_PATH.environment cannot be empty", blankProperty.message)
    }

    @Test
    fun `rejects property placeholders and whitespace in property names`() {
        val placeholder =
            assertFailsWith<IllegalArgumentException> {
                TopicParameterProperties.fromConfigurationValues(
                    values = mapOf("environment" to "${'$'}{kafka.environment}"),
                    path = CONFIGURATION_PATH,
                )
            }
        val whitespace =
            assertFailsWith<IllegalArgumentException> {
                TopicParameterProperties.fromConfigurationValues(
                    values = mapOf("environment" to "kafka environment"),
                    path = CONFIGURATION_PATH,
                )
            }

        assertEquals(
            "$CONFIGURATION_PATH.environment must be a Spring property name without placeholder syntax, " +
                "for example kafka.environment",
            placeholder.message,
        )
        assertEquals("$CONFIGURATION_PATH.environment cannot contain whitespace", whitespace.message)
    }

    private companion object {
        const val CONFIGURATION_PATH = "clientConfig.topicParameterProperties"
    }
}
