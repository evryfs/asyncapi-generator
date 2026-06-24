package dev.banking.asyncapi.generator.core.generator.kotlin.openpayload

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateOpenPayloadTest : AbstractKotlinGeneratorClass() {
    @Test
    fun generate_typealias_for_empty_schema_payload() {
        val generated =
            generateElement(
                yaml = File("src/test/resources/generator/asyncapi_open_payload_empty_schema.yaml"),
                generated = "OpenPayload.kt",
                modelPackage = "dev.banking.asyncapi.generator.core.model.generated.openpayload",
            )
        val typeAlias = extractElement(generated)

        val expected =
            """
            package dev.banking.asyncapi.generator.core.model.generated.openpayload

            typealias OpenPayload = Any
            """.trimIndent()

        assertEquals(expected, typeAlias)
    }

    @Test
    fun generate_typealias_for_open_object_payload() {
        val generated =
            generateElement(
                yaml = File("src/test/resources/generator/asyncapi_open_payload_additional_properties.yaml"),
                generated = "OpenPayload.kt",
                modelPackage = "dev.banking.asyncapi.generator.core.model.generated.openpayload",
            )
        val typeAlias = extractElement(generated)

        val expected =
            """
            package dev.banking.asyncapi.generator.core.model.generated.openpayload

            typealias OpenPayload = Any
            """.trimIndent()

        assertEquals(expected, typeAlias)
    }
}
