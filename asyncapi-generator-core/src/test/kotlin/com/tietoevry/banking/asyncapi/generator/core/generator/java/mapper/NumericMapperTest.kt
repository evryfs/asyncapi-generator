package com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.context.GeneratorContext
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NumericMapperTest {

    private val mapper = NumericMapper()
    private val root = JavaTypeMapper(GeneratorContext(emptyMap()))

    @Test
    fun `map should return Integer for standard integer`() {
        val schema = Schema(type = "integer")
        assertEquals("Integer", mapper.map(schema, "prop", root))
    }

    @Test
    fun `map should return Long for int64 format`() {
        val schema = Schema(type = "integer", format = "int64")
        assertEquals("Long", mapper.map(schema, "prop", root))
    }

    @Test
    fun `map should return Long if maximum exceeds Int MAX_VALUE`() {
        val largeMax = 2147483648L.toBigDecimal()
        val schema = Schema(type = "integer", maximum = largeMax)

        assertEquals("Long", mapper.map(schema, "prop", root))
    }

    @Test
    fun `map should return Double for standard number`() {
        val schema = Schema(type = "number")
        assertEquals("Double", mapper.map(schema, "prop", root))
    }

    @Test
    fun `map should return BigDecimal if multipleOf is present`() {
        // Common pattern for currency to ensure precision
        val schema = Schema(type = "number", multipleOf = 0.01.toBigDecimal())
        assertEquals("BigDecimal", mapper.map(schema, "prop", root))
    }
}
