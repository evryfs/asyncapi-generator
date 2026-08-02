package dev.banking.asyncapi.generator.core.generator.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapperUtilTest {

    @Test
    fun `schema type compatibility normalization remains generator local`() {
        with(MapperUtil) {
            assertEquals("string", " \"string\" ".getPrimaryType())
            assertEquals("integer", listOf("'null'", "|integer").getPrimaryType())
            assertTrue(listOf("'null'", "string").isTypeNullable())
            assertFalse("string".isTypeNullable())
            assertTrue(listOf("string", "integer", "null").hasMultipleNonNullTypes())
            assertFalse(listOf("string", "null").hasMultipleNonNullTypes())
        }
    }
}
