package dev.banking.asyncapi.generator.core.generator.kotlin.enumvalue

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.assertTrue

class GenerateEnumStrictValidationTest : AbstractKotlinGeneratorClass() {
    @Test
    fun `fails when enum literal contains invalid Kotlin identifier character`() {
        val ex =
            assertThrows<AsyncApiGeneratorException.InvalidKotlinEnumLiteral> {
                generateElement(
                    yaml = File("src/test/resources/generator/asyncapi_enum_invalid_symbol.yaml"),
                    generated = "MyField.kt",
                    modelPackage = "dev.banking.asyncapi.generator.core.model.generated.enumstrict",
                )
            }
        assertTrue(ex.message!!.contains("MyField"))
        assertTrue(ex.message!!.contains("SECOND_VALUE?"))
    }
    @Test
    fun `fails when enum literals collide after normalization`() {
        val ex =
            assertThrows<AsyncApiGeneratorException.KotlinEnumLiteralCollision> {
                generateElement(
                    yaml = File("src/test/resources/generator/asyncapi_enum_collision_case.yaml"),
                    generated = "MyField.kt",
                    modelPackage = "dev.banking.asyncapi.generator.core.model.generated.enumstrict",
                )
            }
        assertTrue(ex.message!!.contains("MyField"))
        assertTrue(ex.message!!.contains("OPEN"))
        assertTrue(ex.message!!.contains("open"))
    }
}
