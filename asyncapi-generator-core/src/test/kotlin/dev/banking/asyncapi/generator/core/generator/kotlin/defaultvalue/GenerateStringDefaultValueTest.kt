package dev.banking.asyncapi.generator.core.generator.kotlin.defaultvalue

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateStringDefaultValueTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_data_class_with_string_default_value() {
        val generated =
            generateElement(
                yaml = File("src/test/resources/generator/asyncapi_string_default_value.yaml"),
                generated = "UserSignedUp.kt",
                modelPackage = "dev.banking.asyncapi.generator.core.model.generated.stringdefault",
            )
        val dataClass = extractElement(generated)

        val expected =
            """
            data class UserSignedUp(

                val userId: String = "myString",

                @field:Email
                val email: String,

                val createdAt: OffsetDateTime,

                val referralCode: String? = null
            ) {
            }
            """.trimIndent()

        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_data_class_with_string_default_null_value() {
        val generated =
            generateElement(
                yaml = File("src/test/resources/generator/asyncapi_string_default_null_value.yaml"),
                generated = "UserSignedUp.kt",
                modelPackage = "dev.banking.asyncapi.generator.core.model.generated.stringdefault",
            )
        val dataClass = extractElement(generated)

        val expected =
            """
            data class UserSignedUp(

                val userId: String = null,

                @field:Email
                val email: String,

                val createdAt: OffsetDateTime,

                val referralCode: String? = null
            ) {
            }
            """.trimIndent()

        assertEquals(expected, dataClass)
    }
}
