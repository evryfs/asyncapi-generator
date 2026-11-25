package dev.banking.asyncapi.generator.core.generator.schema

import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaClassificationTest {
    @Test
    fun `recognizes unconstrained payload schemas`() {
        assertTrue(Schema().isOpenPayload())
        assertTrue(
            Schema(
                type = JsonSchemaType.Single("object"),
                additionalProperties = SchemaInterface.BooleanSchema(true),
            ).isOpenPayload(),
        )
        assertTrue(
            Schema(
                type = JsonSchemaType.Single("object"),
                additionalProperties = SchemaInterface.SchemaInline(Schema()),
            ).isOpenPayload(),
        )
    }

    @Test
    fun `rejects payload schemas with declared structure`() {
        assertFalse(
            Schema(
                type = JsonSchemaType.Single("object"),
                properties =
                    mapOf(
                        "id" to SchemaInterface.SchemaInline(Schema(type = JsonSchemaType.Single("string"))),
                    ),
            ).isOpenPayload(),
        )
        assertFalse(
            Schema(
                type = JsonSchemaType.Single("object"),
                additionalProperties = SchemaInterface.BooleanSchema(false),
            ).isOpenPayload(),
        )
    }

    @Test
    fun `recognizes non-enum scalar aliases`() {
        assertTrue(Schema(type = JsonSchemaType.Single("string")).isScalarAlias())
        assertTrue(Schema(type = JsonSchemaType.Single("integer")).isScalarAlias())
        assertTrue(Schema(type = JsonSchemaType.Single("number")).isScalarAlias())
        assertTrue(Schema(type = JsonSchemaType.Single("boolean")).isScalarAlias())
    }

    @Test
    fun `keeps enums and structured schemas as generated models`() {
        assertFalse(Schema(type = JsonSchemaType.Single("string"), enum = listOf("active")).isScalarAlias())
        assertFalse(Schema(type = JsonSchemaType.Single("object")).isScalarAlias())
        assertFalse(Schema().isScalarAlias())
    }
}
