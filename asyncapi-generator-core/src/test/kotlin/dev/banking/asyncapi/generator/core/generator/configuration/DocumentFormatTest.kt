package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DocumentFormatTest {
    @Test
    fun `fromConfigurationValue parses supported document formats`() {
        mapOf(
            "yaml" to DocumentFormat.YAML,
            "json" to DocumentFormat.JSON,
        ).forEach { (configurationValue, expectedFormat) ->
            assertEquals(
                expectedFormat,
                DocumentFormat.fromConfigurationValue(
                    value = configurationValue,
                    path = "generatorName",
                ),
            )
        }
    }

    @Test
    fun `fromConfigurationValue rejects unsupported document format`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                DocumentFormat.fromConfigurationValue(
                    value = "xml",
                    path = "generatorName",
                )
            }

        assertEquals(
            "Invalid generatorName 'xml'. Supported values: yaml, json",
            exception.message,
        )
    }
}
