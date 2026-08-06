package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProducerPayloadTypeTest {
    private val path = "clients.kafka.springKafka.producer.payloadTypes"

    @Test
    fun `omitted values default to contract`() {
        assertEquals(
            setOf(ProducerPayloadType.CONTRACT),
            ProducerPayloadType.fromConfigurationValues(null, path),
        )
    }

    @Test
    fun `configured values are deduplicated in canonical order`() {
        val payloadTypes =
            ProducerPayloadType.fromConfigurationValues(
                listOf("string", "contract", "byte-array", "string"),
                path,
            )

        assertEquals(
            listOf(
                ProducerPayloadType.CONTRACT,
                ProducerPayloadType.BYTE_ARRAY,
                ProducerPayloadType.STRING,
            ),
            payloadTypes.toList(),
        )
    }

    @Test
    fun `invalid and empty values report the path and supported values`() {
        val invalid =
            assertFailsWith<IllegalArgumentException> {
                ProducerPayloadType.fromConfigurationValues(listOf("bytes"), path)
            }
        assertEquals(
            "Invalid $path 'bytes'. Supported values: contract, byte-array, string",
            invalid.message,
        )

        val empty =
            assertFailsWith<IllegalArgumentException> {
                ProducerPayloadType.fromConfigurationValues(emptyList(), path)
            }
        assertEquals(
            "$path cannot be empty. Supported values: contract, byte-array, string",
            empty.message,
        )
    }
}
