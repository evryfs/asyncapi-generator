package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.conditional

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateConditionalTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_asyncapi_conditional_example_ConditionalExample_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_conditional_example.yaml"),
            generated = "ConditionalExample.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.conditional",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class ConditionalExample(

            val type: Type,

            val value: Any
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_asyncapi_conditional_example_Type_enumClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_conditional_example.yaml"),
            generated = "Type.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.conditional",
        )
        val enumClass = extractElement(generated)
        val expected = """
          enum class Type {
              NUMERIC,
              TEXT,
          }
        """.trimIndent()
        assertEquals(expected, enumClass)
    }
}
