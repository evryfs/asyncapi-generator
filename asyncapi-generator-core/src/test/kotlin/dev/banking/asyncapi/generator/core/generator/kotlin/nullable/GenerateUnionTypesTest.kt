package dev.banking.asyncapi.generator.core.generator.kotlin.nullable

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateUnionTypesTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_union_types_with_strict_json_schema_semantics() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_union_types.yaml"),
            generated = "UnionTypes.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.union",
        )
        val dataClass = extractElement(generated)
        val expected = """
           data class UnionTypes(

               val stringOrArray: Any,

               val arrayOrNull: List<String>? = null,

               val stringArrayOrNull: Any? = null,

               val stringOrNull: String? = null
           ) {
           }
           """.trimIndent()
        assertEquals(expected, dataClass)
    }
}
