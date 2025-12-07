package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ConstraintMapperTest {

    private val mapper = ConstraintMapper()

    @Test
    fun `buildAnnotations should generate size constraints`() {
        val schema = Schema(type = "string", minLength = 5, maxLength = 10)
        val anns = mapper.buildAnnotations(schema)

        assertTrue(anns.any { it.contains("@field:Size(min = 5, max = 10)") })
    }

    @Test
    fun `buildAnnotations should generate pattern constraint`() {
        val schema = Schema(type = "string", pattern = "^[A-Z]+$")
        val anns = mapper.buildAnnotations(schema)

        assertTrue(anns.contains("@field:Pattern(regexp = \"^[A-Z]+$\")"))
    }

    @Test
    fun `buildAnnotations should generate email constraint`() {
        val schema = Schema(type = "string", format = "email")
        val anns = mapper.buildAnnotations(schema)

        assertTrue(anns.contains("@field:Email"))
    }

    @Test
    fun `buildAnnotations should generate min max for integers`() {
        val schema = Schema(type = "integer", minimum = 0.toBigDecimal(), maximum = 100.toBigDecimal())
        val anns = mapper.buildAnnotations(schema)

        assertTrue(anns.contains("@field:Min(0L)"))
        assertTrue(anns.contains("@field:Max(100L)"))
    }

    @Test
    fun `buildAnnotations should generate decimal min max`() {
        val schema = Schema(type = "number", minimum = 10.5.toBigDecimal())
        val anns = mapper.buildAnnotations(schema)

        assertTrue(anns.contains("@field:DecimalMin(value = \"10.5\", inclusive = true)"))
    }
}
