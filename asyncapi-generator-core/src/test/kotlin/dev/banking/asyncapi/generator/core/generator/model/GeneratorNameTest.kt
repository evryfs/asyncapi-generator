package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorProfile
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaType
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
            GeneratorName.AVRO_SCHEMA,
            GeneratorName.fromConfigurationValue(
                value = "avro-schema",
                path = "generatorName",
            ),
        )
        assertEquals(
            GeneratorName.PROTOBUF_SCHEMA,
            GeneratorName.fromConfigurationValue(
                value = "protobuf-schema",
                path = "generatorName",
            ),
        )
        assertEquals(
            GeneratorName.JSON_SCHEMA,
            GeneratorName.fromConfigurationValue(
                value = "json-schema",
                path = "generatorName",
            ),
        )
    }

    @Test
    fun `generator names expose typed source profiles`() {
        assertEquals(
            GeneratorProfile.Source(SourceLanguage.JAVA),
            GeneratorName.JAVA.profile,
        )
        assertEquals(
            GeneratorProfile.Source(SourceLanguage.KOTLIN),
            GeneratorName.KOTLIN.profile,
        )
    }

    @Test
    fun `schema generator names expose typed schema profiles`() {
        assertEquals(
            GeneratorProfile.Schema(SchemaType.AVRO),
            GeneratorName.AVRO_SCHEMA.profile,
        )
        assertEquals(
            GeneratorProfile.Schema(SchemaType.PROTOBUF),
            GeneratorName.PROTOBUF_SCHEMA.profile,
        )
        assertEquals(
            GeneratorProfile.Schema(SchemaType.JSON_SCHEMA),
            GeneratorName.JSON_SCHEMA.profile,
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
            "Invalid generatorName 'python'. Supported values: java, kotlin, avro-schema, protobuf-schema, json-schema",
            exception.message,
        )
    }
}
