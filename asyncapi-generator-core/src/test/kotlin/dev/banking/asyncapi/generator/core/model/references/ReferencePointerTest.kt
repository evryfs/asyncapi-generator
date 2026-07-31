package dev.banking.asyncapi.generator.core.model.references

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReferencePointerTest {
    @Test
    fun `extracts a component schema name from the reference fragment`() {
        assertEquals(
            "ExternalPayload",
            "./schemas.yaml#/components/schemas/ExternalPayload".componentSchemaNameOrNull(),
        )
    }

    @Test
    fun `does not treat a matching document path as a component schema pointer`() {
        assertNull("./components/schemas.yaml#/ExternalPayload".componentSchemaNameOrNull())
    }

    @Test
    fun `decodes the component schema JSON Pointer segment`() {
        assertEquals(
            "External/Payload~V1",
            "./schemas.yaml#/components/schemas/External~1Payload~0V1".componentSchemaNameOrNull(),
        )
    }
}
