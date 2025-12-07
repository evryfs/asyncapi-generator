package dev.banking.asyncapi.generator.core.generator.kotlin.array

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateArrayTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_asyncapi_array_primitive_object_ContactPointType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_array_primitive_object.yaml"),
            generated = "ContactPointType.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.array",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class ContactPointType(

            @field:Size(min = 3, max = 20)
            val type: String,

            @field:Size(max = 200)
            val value: String,

            val preferred: Boolean? = null
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_asyncapi_array_primitive_object_CustomerWithContacts_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_array_primitive_object.yaml"),
            generated = "CustomerWithContacts.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.array",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class CustomerWithContacts(

            val customerId: UUID,

            @field:Size(max = 140)
            val fullName: String,

            val tags: List<String>? = null,

            @field:Valid
            val contactPoints: List<ContactPointType>
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_enum_from_array_items() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_array_inline_enum.yaml"),
            generated = "Priorities.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.arrayenum",
        )
        val enumClass = extractElement(generated)

        val expected = """
           enum class Priorities {
               LOW,
               MEDIUM,
               HIGH,
           }
           """.trimIndent()

        assertEquals(expected, enumClass)
    }

    @Test
    fun generate_data_class_with_list_of_enums() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_array_inline_enum.yaml"),
            generated = "ObjectWithArrayOfEnums.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.arrayenum",
        )
        val dataClass = extractElement(generated)

        val expected = """
           data class ObjectWithArrayOfEnums(

               val priorities: List<Priorities>? = null
           ) {
           }
           """.trimIndent()

        assertEquals(expected, dataClass)
    }
}
