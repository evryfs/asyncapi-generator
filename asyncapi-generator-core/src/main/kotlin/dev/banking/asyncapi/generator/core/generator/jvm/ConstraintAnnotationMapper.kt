package dev.banking.asyncapi.generator.core.generator.jvm

import dev.banking.asyncapi.generator.core.generator.sourcemodel.Constraints

/** Expresses validation constraints as Jakarta Validation annotations. */
internal object ConstraintAnnotationMapper {
    private const val CONSTRAINTS = "jakarta.validation.constraints"

    /**
     * The annotations of [constraints].
     *
     * @param constraints the constraints of a property or parameter
     * @param stringLiteral escapes text for a string literal of the annotated source
     */
    fun buildAnnotations(
        constraints: Constraints,
        stringLiteral: (String) -> String,
    ): List<SourceAnnotation> =
        when (constraints) {
            is Constraints.Text -> textAnnotations(constraints, stringLiteral)
            is Constraints.WholeNumber ->
                listOfNotNull(
                    constraints.minimum?.let { annotation("Min", "${it}L") },
                    constraints.maximum?.let { annotation("Max", "${it}L") },
                )
            is Constraints.Decimal ->
                listOfNotNull(
                    constraints.minimum?.let {
                        annotation("DecimalMin", "value = \"${it.value}\", inclusive = ${it.inclusive}")
                    },
                    constraints.maximum?.let {
                        annotation("DecimalMax", "value = \"${it.value}\", inclusive = ${it.inclusive}")
                    },
                )
            Constraints.None -> emptyList()
        }

    private fun textAnnotations(
        constraints: Constraints.Text,
        stringLiteral: (String) -> String,
    ): List<SourceAnnotation> {
        val sizeArguments =
            listOfNotNull(
                constraints.minLength?.let { "min = $it" },
                constraints.maxLength?.let { "max = $it" },
            )
        return listOfNotNull(
            sizeArguments.takeIf { it.isNotEmpty() }?.let { annotation("Size", it.joinToString(", ")) },
            constraints.pattern?.let { annotation("Pattern", "regexp = \"${stringLiteral(it)}\"") },
            annotation("Email").takeIf { constraints.email },
        )
    }

    private fun annotation(
        name: String,
        arguments: String? = null,
    ) = SourceAnnotation(type = "$CONSTRAINTS.$name", arguments = arguments)
}
