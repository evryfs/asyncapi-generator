package dev.banking.asyncapi.generator.core.model.schemas

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonSchemaTypeTest {
    @Test
    fun `reads the main type, null, and type unions from single and listed names`() {
        val single = JsonSchemaType.Single("string")
        val nullable = JsonSchemaType.Multiple(listOf("null", "integer"))
        val union = JsonSchemaType.Multiple(listOf("string", "integer", "null"))

        assertEquals("string", single.primary)
        assertFalse(single.allowsNull)
        assertEquals("integer", nullable.primary)
        assertTrue(nullable.allowsNull)
        assertFalse(nullable.hasMultipleNonNullTypes)
        assertTrue(union.hasMultipleNonNullTypes)
        assertNull(JsonSchemaType.Multiple(listOf("null")).primary)
    }
}
