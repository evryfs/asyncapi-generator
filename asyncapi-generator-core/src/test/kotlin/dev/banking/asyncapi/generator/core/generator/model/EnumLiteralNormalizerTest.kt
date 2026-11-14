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
    fun `maps values to upper case constants and keeps differing wire values`() {
        assertEquals(
            listOf(
                EnumConstant("OPEN", jsonValue = null),
                EnumConstant("CLOSED", jsonValue = "closed"),
                EnumConstant("IN_PROGRESS", jsonValue = "in-progress"),
                EnumConstant("_2FA", jsonValue = "2fa"),
            ),
            normalizer.normalize("Status", listOf("OPEN", "closed", "in-progress", "2fa")),
        )
    }

    @Test
    fun `rejects a value that leaves no identifier`() {
        val exception =
            assertThrows<InvalidEnum> {
                normalizer.normalize("Status", listOf("--"))
            }

        assertTrue(exception.message!!.contains("Status"))
        assertTrue(exception.message!!.contains("'--'"))
    }

    @Test
    fun `rejects literals that collide after normalization`() {
        val exception =
            assertThrows<EnumLiteralCollision> {
                normalizer.normalize("Status", listOf("in-progress", "IN_PROGRESS"))
            }

        assertTrue(exception.message!!.contains("Status"))
        assertTrue(exception.message!!.contains("'in-progress'"))
        assertTrue(exception.message!!.contains("'IN_PROGRESS'"))
        assertTrue(exception.message!!.contains("-> 'IN_PROGRESS'"))
    }
}
