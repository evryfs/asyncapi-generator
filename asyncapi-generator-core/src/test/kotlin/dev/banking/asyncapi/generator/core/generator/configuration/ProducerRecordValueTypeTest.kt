package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProducerRecordValueTypeTest {
    @Test
    fun `fromConfigurationValue defaults to byte array`() {
        assertEquals(
            ProducerRecordValueType.ByteArray,
            ProducerRecordValueType.fromConfigurationValue(
                value = null,
                path = "clientConfig.producer.recordValueType",
            ),
        )
    }

    @Test
    fun `fromConfigurationValue creates custom record value type`() {
        assertEquals(
            ProducerRecordValueType.Custom(
                QualifiedTypeName.fromConfigurationValue(
                    value = "org.apache.avro.specific.SpecificRecord",
                    path = "clientConfig.producer.recordValueType",
                ),
            ),
            ProducerRecordValueType.fromConfigurationValue(
                value = "org.apache.avro.specific.SpecificRecord",
                path = "clientConfig.producer.recordValueType",
            ),
        )
    }

    @Test
    fun `fromConfigurationValue rejects unqualified custom type`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ProducerRecordValueType.fromConfigurationValue(
                    value = "SpecificRecord",
                    path = "clientConfig.producer.recordValueType",
                )
            }

        assertEquals(
            "clientConfig.producer.recordValueType must be a fully qualified type name, " +
                "for example com.example.GeneratedPayload",
            exception.message,
        )
    }
}
