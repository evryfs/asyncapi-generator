package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ClientTypeTest {
    @Test
    fun `fromConfigurationValue parses supported client type`() {
        assertEquals(
            ClientType.SPRING_KAFKA,
            ClientType.fromConfigurationValue(
                value = "spring-kafka",
                path = "clientConfig.clientType",
            ),
        )
    }

    @Test
    fun `fromConfigurationValue requires client type`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ClientType.fromConfigurationValue(
                    value = null,
                    path = "clientConfig.clientType",
                )
            }

        assertEquals("clientConfig.clientType is required", exception.message)
    }

    @Test
    fun `fromConfigurationValue rejects unsupported client type`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ClientType.fromConfigurationValue(
                    value = "spring-mq",
                    path = "clientConfig.clientType",
                )
            }

        assertEquals(
            "Invalid clientConfig.clientType 'spring-mq'. Supported values: spring-kafka",
            exception.message,
        )
    }
}
