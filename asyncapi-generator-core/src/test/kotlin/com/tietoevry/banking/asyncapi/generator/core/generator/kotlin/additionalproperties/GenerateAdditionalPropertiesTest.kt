package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.additionalproperties

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateAdditionalPropertiesTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_data_class_with_map_properties() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_additionalproperties_map_objects.yaml"),
            generated = "ContainerObject.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.additionalproperties",
        )
        val dataClass = extractElement(generated)

        val expected = """
           data class ContainerObject(

               val stringTags: Map<String, String>? = null,

               val anyTags: Map<String, Any>? = null,

               @field:Valid
               val itemMap: Map<String, SomeItem>? = null
           ) {
           }
           """.trimIndent()

        assertEquals(expected, dataClass)
    }
}
