package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.EnumLiteralCollision
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidEnum
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnumLiteralNormalizerTest {
    private val normalizer = EnumLiteralNormalizer("com.example.model")

    @Test
    fun `normalizes supported enum literals`() {
        assertEquals(
            listOf("OPEN", "CLOSED", "PENDING"),
            normalizer.normalize("Status", listOf("OPEN", "closed", "PENDING")),
        )
    }

    @Test
    fun `rejects an invalid normalized identifier`() {
        val exception =
            assertThrows<InvalidEnum> {
                normalizer.normalize("Status", listOf("SECOND_VALUE?"))
            }

        assertTrue(exception.message!!.contains("Status"))
        assertTrue(exception.message!!.contains("SECOND_VALUE?"))
        assertTrue(exception.message!!.contains("[A-Z_][A-Z0-9_]*"))
    }

    @Test
    fun `rejects literals that collide after normalization`() {
        val exception =
            assertThrows<EnumLiteralCollision> {
                normalizer.normalize("Status", listOf("OPEN", "open"))
            }

        assertTrue(exception.message!!.contains("Status"))
        assertTrue(exception.message!!.contains("'OPEN'"))
        assertTrue(exception.message!!.contains("'open'"))
        assertTrue(exception.message!!.contains("-> 'OPEN'"))
    }
}
