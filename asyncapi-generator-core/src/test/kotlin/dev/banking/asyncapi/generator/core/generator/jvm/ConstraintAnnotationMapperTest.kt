package dev.banking.asyncapi.generator.core.generator.jvm

import dev.banking.asyncapi.generator.core.generator.sourcemodel.Constraints
import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ConstraintAnnotationMapperTest {
    private fun java(constraints: Constraints) =
        ConstraintAnnotationMapper.buildAnnotations(constraints, SourceLiteralEscaper::forJava).map { it.render() }

    private fun kotlinField(constraints: Constraints) =
        ConstraintAnnotationMapper
            .buildAnnotations(
                constraints,
                SourceLiteralEscaper::forKotlin,
            ).map { it.render("field") }

    @Test
    fun `maps string constraints with source-specific targets and escaping`() {
        val schema =
            Schema(
                type = JsonSchemaType.Single("string"),
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
            java(Constraints.of(schema)),
        )
        assertEquals(
            listOf(
                "@field:Size(min = 5, max = 10)",
                "@field:Pattern(regexp = \" ^\\\\d+\\\"quoted\\\"\\${'$'}value \")",
                "@field:Email",
            ),
            kotlinField(Constraints.of(schema)),
        )
    }

    @Test
    fun `imports each annotation by its Jakarta Validation type`() {
        val annotations =
            ConstraintAnnotationMapper.buildAnnotations(
                Constraints.Text(minLength = 1, maxLength = null, pattern = null, email = true),
                SourceLiteralEscaper::forJava,
            )

        assertEquals(
            listOf("jakarta.validation.constraints.Size", "jakarta.validation.constraints.Email"),
            annotations.flatMap(SourceAnnotation::imports),
        )
    }

    @Test
    fun `maps integer bounds`() {
        val schema =
            Schema(type = JsonSchemaType.Single("integer"), minimum = 0.toBigDecimal(), maximum = 100.toBigDecimal())

        assertEquals(listOf("@Min(0L)", "@Max(100L)"), java(Constraints.of(schema)))
    }

    @Test
    fun `prefers exclusive decimal bounds`() {
        val schema =
            Schema(
                type = JsonSchemaType.Single("number"),
                minimum = 10.5.toBigDecimal(),
                exclusiveMinimum = 10.6.toBigDecimal(),
                maximum = 20.5.toBigDecimal(),
            )

        assertEquals(
            listOf(
                "@DecimalMin(value = \"10.6\", inclusive = false)",
                "@DecimalMax(value = \"20.5\", inclusive = true)",
            ),
            java(Constraints.of(schema)),
        )
    }
}
