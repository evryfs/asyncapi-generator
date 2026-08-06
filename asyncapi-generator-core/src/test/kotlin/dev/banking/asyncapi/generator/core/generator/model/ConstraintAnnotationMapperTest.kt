package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ConstraintAnnotationMapperTest {
    private val javaMapper = ConstraintAnnotationMapper(SourceLanguage.JAVA)
    private val kotlinMapper = ConstraintAnnotationMapper(SourceLanguage.KOTLIN)

    @Test
    fun `maps string constraints with source-specific targets and escaping`() {
        val schema =
            Schema(
                type = "string",
                minLength = 5,
                maxLength = 10,
                pattern = " ^\\d+\"quoted\"${'$'}value ",
                format = "email",
            )

        assertEquals(
            listOf(
                "@Size(min = 5, max = 10)",
                "@Pattern(regexp = \" ^\\\\d+\\\"quoted\\\"${'$'}value \")",
                "@Email",
            ),
            javaMapper.buildAnnotations(schema),
        )
        assertEquals(
            listOf(
                "@field:Size(min = 5, max = 10)",
                "@field:Pattern(regexp = \" ^\\\\d+\\\"quoted\\\"\\${'$'}value \")",
                "@field:Email",
            ),
            kotlinMapper.buildAnnotations(schema),
        )
    }

    @Test
    fun `maps integer bounds`() {
        val schema = Schema(type = "integer", minimum = 0.toBigDecimal(), maximum = 100.toBigDecimal())

        assertEquals(listOf("@Min(0L)", "@Max(100L)"), javaMapper.buildAnnotations(schema))
    }

    @Test
    fun `prefers exclusive decimal bounds`() {
        val schema =
            Schema(
                type = "number",
                minimum = 10.5.toBigDecimal(),
                exclusiveMinimum = 10.6.toBigDecimal(),
                maximum = 20.5.toBigDecimal(),
            )

        assertEquals(
            listOf(
                "@DecimalMin(value = \"10.6\", inclusive = false)",
                "@DecimalMax(value = \"20.5\", inclusive = true)",
            ),
            javaMapper.buildAnnotations(schema),
        )
    }
}
