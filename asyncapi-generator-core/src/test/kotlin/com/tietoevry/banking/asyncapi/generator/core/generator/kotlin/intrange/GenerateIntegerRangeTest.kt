package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.intrange

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateIntegerRangeTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_asyncapi_integer_range_type_IntegerRangesType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_integer_range_type.yaml"),
            generated = "IntegerRangesType.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.intrange",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class IntegerRangesType(

            @field:Min(0L)
            @field:Max(100000L)
            val smallCounter: Int,

            @field:Min(0L)
            @field:Max(9007199254740991L)
            val largeCounter: Long,

            @field:Min(-1000L)
            @field:Max(1000L)
            val boundedWithoutFormat: Int? = null,

            val unboundedInteger: Int? = null
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }
}
