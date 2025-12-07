package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.nullable

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateNullableTypesTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_nullable_object_with_various_nullable_fields() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_nullable_types.yaml"),
            generated = "NullableObject.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.nullable",
        )
        val dataClass = extractElement(generated)

        val expected = """
           data class NullableObject(

               val requiredString: String,

               val optionalString: String? = null,

               val nullableStringArray: List<String>? = null,

               val stringOrNull: String? = null,

               val integerOrNull: Int? = null,

               val booleanOrNull: Boolean? = null
           ) {
           }
           """.trimIndent()

        assertEquals(expected, dataClass)
    }
}
