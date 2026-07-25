package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModelTypeTest {
    @Test
    fun `fromConfigurationValue parses supported model types`() {
        mapOf(
            "kotlin-data-class" to ModelType.KOTLIN_DATA_CLASS,
            "java-class" to ModelType.JAVA_CLASS,
            "java-record" to ModelType.JAVA_RECORD,
            "avro-specific-record" to ModelType.AVRO_SPECIFIC_RECORD,
            "protobuf-message" to ModelType.PROTOBUF_MESSAGE,
        ).forEach { (configurationValue, expectedType) ->
            assertEquals(
                expectedType,
                ModelType.fromConfigurationValue(
                    value = configurationValue,
                    path = "modelConfig.modelType",
                ),
            )
        }
    }

    @Test
    fun `fromConfigurationValue rejects unsupported model type`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ModelType.fromConfigurationValue(
                    value = "data-class",
                    path = "modelConfig.modelType",
                )
            }

        assertEquals(
            "Invalid modelConfig.modelType 'data-class'. Supported values: " +
                "kotlin-data-class, java-class, java-record, avro-specific-record, protobuf-message",
            exception.message,
        )
    }
}
