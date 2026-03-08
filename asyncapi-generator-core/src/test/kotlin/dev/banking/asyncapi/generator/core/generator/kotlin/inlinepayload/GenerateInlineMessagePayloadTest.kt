package dev.banking.asyncapi.generator.core.generator.kotlin.inlinepayload

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateInlineMessagePayloadTest : AbstractKotlinGeneratorClass() {
    @Test
    fun generate_data_class_for_inline_payload_with_properties() {
        val generated =
            generateElement(
                yaml = File("src/test/resources/generator/asyncapi_inline_message_payload_properties.yaml"),
                generated = "UserSignupPayload.kt",
                modelPackage = "dev.banking.asyncapi.generator.core.model.generated.inlinepayload",
            )
        val dataClass = extractElement(generated)

        val expected =
            """
            data class UserSignupPayload(

                @field:Valid
                val user: UserCreate? = null,

                @field:Valid
                val signup: Signup? = null
            ) {
            }
            """.trimIndent()

        assertEquals(expected, dataClass)
    }
}
