package dev.banking.asyncapi.generator.core.generator.schema

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
                type = "object",
                additionalProperties = SchemaInterface.BooleanSchema(true),
            ).isOpenPayload(),
        )
        assertTrue(
            Schema(
                type = "object",
                additionalProperties = SchemaInterface.SchemaInline(Schema()),
            ).isOpenPayload(),
        )
    }

    @Test
    fun `rejects payload schemas with declared structure`() {
        assertFalse(
            Schema(
                type = "object",
                properties =
                    mapOf(
                        "id" to SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
            ).isOpenPayload(),
        )
        assertFalse(
            Schema(
                type = "object",
                additionalProperties = SchemaInterface.BooleanSchema(false),
            ).isOpenPayload(),
        )
    }

    @Test
    fun `recognizes non-enum scalar aliases`() {
        assertTrue(Schema(type = "string").isScalarAlias())
        assertTrue(Schema(type = "integer").isScalarAlias())
        assertTrue(Schema(type = "number").isScalarAlias())
        assertTrue(Schema(type = "boolean").isScalarAlias())
    }

    @Test
    fun `keeps enums and structured schemas as generated models`() {
        assertFalse(Schema(type = "string", enum = listOf("active")).isScalarAlias())
        assertFalse(Schema(type = "object").isScalarAlias())
        assertFalse(Schema().isScalarAlias())
    }
}
