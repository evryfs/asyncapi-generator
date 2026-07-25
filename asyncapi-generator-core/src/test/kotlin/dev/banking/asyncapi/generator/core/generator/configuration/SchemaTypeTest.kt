package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SchemaTypeTest {
    @Test
    fun `fromConfigurationValue parses supported schema types`() {
        mapOf(
            "json-schema" to SchemaType.JSON_SCHEMA,
            "avro" to SchemaType.AVRO,
            "protobuf" to SchemaType.PROTOBUF,
        ).forEach { (configurationValue, expectedType) ->
            assertEquals(
                expectedType,
                SchemaType.fromConfigurationValue(
                    value = configurationValue,
                    path = "schemaConfig.schemaType",
                ),
            )
        }
    }

    @Test
    fun `fromConfigurationValue rejects unsupported schema type`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SchemaType.fromConfigurationValue(
                    value = "openapi",
                    path = "schemaConfig.schemaType",
                )
            }

        assertEquals(
            "Invalid schemaConfig.schemaType 'openapi'. Supported values: json-schema, avro, protobuf",
            exception.message,
        )
    }
}
