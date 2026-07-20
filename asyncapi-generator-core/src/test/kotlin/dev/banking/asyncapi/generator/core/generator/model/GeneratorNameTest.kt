package dev.banking.asyncapi.generator.core.generator.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GeneratorNameTest {
    @Test
    fun `fromConfigurationValue requires generator name`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorName.fromConfigurationValue(
                    value = null,
                    path = "generatorName",
                )
            }

        assertEquals("generatorName is required", exception.message)
    }

    @Test
    fun `fromConfigurationValue parses supported configuration values`() {
        assertEquals(
            GeneratorName.JAVA,
            GeneratorName.fromConfigurationValue(
                value = "java",
                path = "generatorName",
            ),
        )
        assertEquals(
            GeneratorName.KOTLIN,
            GeneratorName.fromConfigurationValue(
                value = "kotlin",
                path = "generatorName",
            ),
        )
        assertEquals(
            GeneratorName.AVRO,
            GeneratorName.fromConfigurationValue(
                value = "avro",
                path = "generatorName",
            ),
        )
        assertEquals(
            GeneratorName.PROTOBUF,
            GeneratorName.fromConfigurationValue(
                value = "protobuf",
                path = "generatorName",
            ),
        )
    }

    @Test
    fun `fromConfigurationValue rejects unsupported configuration values`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorName.fromConfigurationValue(
                    value = "python",
                    path = "generatorName",
                )
            }

        assertEquals(
            "Invalid generatorName 'python'. Supported values: java, kotlin, avro, protobuf",
            exception.message,
        )
    }
}
