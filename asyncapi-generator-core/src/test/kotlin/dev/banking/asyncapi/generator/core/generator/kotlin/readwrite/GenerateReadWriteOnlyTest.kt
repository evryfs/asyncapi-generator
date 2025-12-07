package dev.banking.asyncapi.generator.core.generator.kotlin.readwrite

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateReadWriteOnlyTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_data_class_with_readOnly_writeOnly_fields() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_read_write_only.yaml"),
            generated = "AccessControlledObject.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.access",
        )
        val dataClass = extractElement(generated)
        val expected = """
           data class AccessControlledObject(

               @JsonProperty(access = Access.READ_ONLY)
               val id: String? = null,

               @JsonProperty(access = Access.WRITE_ONLY)
               val password: String? = null,

               val status: String? = null,

               @JsonProperty(access = Access.READ_WRITE)
               val secretKey: String? = null
           ) {
           }
           """.trimIndent()

        assertEquals(expected, dataClass)
    }
}
