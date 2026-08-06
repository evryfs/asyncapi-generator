package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdditionalProducerPayloadTypeTest {
    private val path = "clientConfig.producer.additionalPayloadTypes"

    @Test
    fun `omitted and empty values configure no additional payload types`() {
        assertEquals(
            emptySet(),
            AdditionalProducerPayloadType.fromConfigurationValues(null, path),
        )
        assertEquals(
            emptySet(),
            AdditionalProducerPayloadType.fromConfigurationValues(emptyList(), path),
        )
    }

    @Test
    fun `configured values are deduplicated in canonical order`() {
        val payloadTypes =
            AdditionalProducerPayloadType.fromConfigurationValues(
                listOf("string", "byte-array", "string"),
                path,
            )

        assertEquals(
            listOf(
                AdditionalProducerPayloadType.BYTE_ARRAY,
                AdditionalProducerPayloadType.STRING,
            ),
            payloadTypes.toList(),
        )
    }

    @Test
    fun `invalid values report the path and supported values`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                AdditionalProducerPayloadType.fromConfigurationValues(listOf("contract"), path)
            }

        assertEquals(
            "Invalid $path 'contract'. Supported values: byte-array, string",
            exception.message,
        )
    }
}
