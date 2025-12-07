package dev.banking.asyncapi.generator.core.generator.kotlin.constraint

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateConstraintTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_asyncapi_string_constraints_type_StringConstraintsType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_string_constraints_type.yaml"),
            generated = "StringConstraintsType.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.constraint",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class StringConstraintsType(

            @field:Size(max = 255)
            @field:Email
            val email: String,

            @field:Size(min = 3, max = 30)
            @field:Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]*")
            val username: String,

            @field:Size(max = 500)
            val freeText: String? = null,

            @field:Size(min = 2, max = 2)
            @field:Pattern(regexp = "[A-Z]{2}")
            val countryCode: String? = null
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }
}
