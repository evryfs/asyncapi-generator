package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ClientContractTest {
    @Test
    fun `fromConfigurationValue parses supported client contract`() {
        assertEquals(
            ClientContract.INTERFACE,
            ClientContract.fromConfigurationValue(
                value = "interface",
                path = "clientConfig.clientContract",
            ),
        )
    }

    @Test
    fun `fromConfigurationValue requires client contract`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ClientContract.fromConfigurationValue(
                    value = null,
                    path = "clientConfig.clientContract",
                )
            }

        assertEquals("clientConfig.clientContract is required", exception.message)
    }

    @Test
    fun `fromConfigurationValue rejects unsupported client contract`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ClientContract.fromConfigurationValue(
                    value = "class",
                    path = "clientConfig.clientContract",
                )
            }

        assertEquals(
            "Invalid clientConfig.clientContract 'class'. Supported values: interface",
            exception.message,
        )
    }
}
