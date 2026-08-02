package dev.banking.asyncapi.generator.core.validator.schemas

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaValueSemanticsTest {

    @Test
    fun `numeric and nested JSON values use mathematical equality`() {
        val first = mapOf("values" to listOf(1, BigInteger("100000000000000000000000000000000000000")))
        val duplicate = mapOf(
            "values" to listOf(BigDecimal("1.0"), BigDecimal("100000000000000000000000000000000000000.0")),
        )

        assertTrue(SchemaValueSemantics.hasDuplicates(listOf(first, duplicate)))
        assertFalse(SchemaValueSemantics.hasDuplicates(listOf("value", " value ")))
    }

    @Test
    fun `integer compatibility retains arbitrary precision and explicit null semantics`() {
        assertTrue(
            SchemaValueSemantics.isCompatible(
                BigInteger("100000000000000000000000000000000000000"),
                "integer",
            ),
        )
        assertTrue(SchemaValueSemantics.isCompatible(null, listOf("string", "null")))
        assertFalse(SchemaValueSemantics.isCompatible(null, "string"))
    }
}
