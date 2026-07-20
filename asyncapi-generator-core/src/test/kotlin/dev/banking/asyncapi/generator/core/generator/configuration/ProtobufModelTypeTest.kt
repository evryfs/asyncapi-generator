package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProtobufModelTypeTest {
    @Test
    fun `fromConfigurationValue defaults to Java when value is not configured`() {
        assertEquals(
            ProtobufModelType.JAVA,
            ProtobufModelType.fromConfigurationValue(
                value = null,
                path = "models.protobufModelType",
            ),
        )
    }

    @Test
    fun `fromConfigurationValue parses supported configuration values`() {
        assertEquals(
            ProtobufModelType.JAVA,
            ProtobufModelType.fromConfigurationValue(
                value = "java",
                path = "models.protobufModelType",
            ),
        )
        assertEquals(
            ProtobufModelType.KOTLIN,
            ProtobufModelType.fromConfigurationValue(
                value = "kotlin",
                path = "models.protobufModelType",
            ),
        )
    }

    @Test
    fun `fromConfigurationValue rejects unsupported configuration values`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ProtobufModelType.fromConfigurationValue(
                    value = "data-class",
                    path = "models.protobufModelType",
                )
            }

        assertEquals(
            "Invalid models.protobufModelType 'data-class'. Supported values: java, kotlin",
            exception.message,
        )
    }
}
