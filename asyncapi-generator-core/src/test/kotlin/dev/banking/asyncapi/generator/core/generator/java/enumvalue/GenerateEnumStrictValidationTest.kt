package dev.banking.asyncapi.generator.core.generator.java.enumvalue

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.assertTrue

class GenerateEnumStrictValidationTest : AbstractJavaGeneratorClass() {
    @Test
    fun `fails when enum literal contains invalid Java identifier character`() {
        val ex =
            assertThrows<AsyncApiGeneratorException.InvalidEnum> {
                generateElement(
                    yaml = File("src/test/resources/generator/asyncapi_enum_invalid_symbol.yaml"),
                    generated = "MyField.java",
                    modelPackage = "dev.banking.asyncapi.generator.core.model.generated.enumstrict",
                )
            }
        assertTrue(ex.message!!.contains("MyField"))
        assertTrue(ex.message!!.contains("SECOND_VALUE?"))
        assertTrue(ex.message!!.contains("[A-Z_][A-Z0-9_]*"))
    }

    @Test
    fun `fails when enum literals collide after normalization`() {
        val ex =
            assertThrows<AsyncApiGeneratorException.EnumLiteralCollision> {
                generateElement(
                    yaml = File("src/test/resources/generator/asyncapi_enum_collision_case.yaml"),
                    generated = "MyField.java",
                    modelPackage = "dev.banking.asyncapi.generator.core.model.generated.enumstrict",
                )
            }
        assertTrue(ex.message!!.contains("MyField"))
        assertTrue(ex.message!!.contains("OPEN"))
        assertTrue(ex.message!!.contains("open"))
    }
}
