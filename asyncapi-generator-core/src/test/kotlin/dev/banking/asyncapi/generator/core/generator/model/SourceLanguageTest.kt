package dev.banking.asyncapi.generator.core.generator.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceLanguageTest {
    @Test
    fun `fromConfigurationValue defaults to kotlin when value is not configured`() {
        assertEquals(
            SourceLanguage.KOTLIN,
            SourceLanguage.fromConfigurationValue(
                value = null,
                path = "generatorName",
            ),
        )
    }

    @Test
    fun `fromConfigurationValue parses supported source languages`() {
        assertEquals(
            SourceLanguage.KOTLIN,
            SourceLanguage.fromConfigurationValue(
                value = "kotlin",
                path = "generatorName",
            ),
        )
        assertEquals(
            SourceLanguage.JAVA,
            SourceLanguage.fromConfigurationValue(
                value = "java",
                path = "generatorName",
            ),
        )
    }

    @Test
    fun `fromConfigurationValue rejects unsupported source languages`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SourceLanguage.fromConfigurationValue(
                    value = "python",
                    path = "generatorName",
                )
            }

        assertEquals(
            "Invalid generatorName 'python'. Supported values: kotlin, java",
            exception.message,
        )
    }
}
