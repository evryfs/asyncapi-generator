package dev.banking.asyncapi.generator.core.generator.schema

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaTypesTest {
    @Test
    fun `reads the schema type ignoring quotes and null`() {
        assertEquals("string", " \"string\" ".getPrimaryType())
        assertEquals("integer", listOf("'null'", "|integer").getPrimaryType())
        assertTrue(listOf("'null'", "string").isTypeNullable())
        assertFalse("string".isTypeNullable())
        assertTrue(listOf("string", "integer", "null").hasMultipleNonNullTypes())
        assertFalse(listOf("string", "null").hasMultipleNonNullTypes())
    }
}
