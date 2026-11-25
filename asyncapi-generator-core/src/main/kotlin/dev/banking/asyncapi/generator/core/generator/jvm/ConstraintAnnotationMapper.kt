package dev.banking.asyncapi.generator.core.generator.jvm

import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.ir.Constraints

/**
 * Expresses validation constraints as Jakarta Validation annotations, written for fields of the
 * given language.
 *
 * @param sourceLanguage Language of the annotated source, which decides the use-site target and escaping.
 */
internal class ConstraintAnnotationMapper(
    private val sourceLanguage: SourceLanguage,
) {
    fun buildAnnotations(constraints: Constraints): List<String> =
        when (constraints) {
            is Constraints.Text -> textAnnotations(constraints)
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

    private fun textAnnotations(constraints: Constraints.Text): List<String> {
        val sizeArguments =
            listOfNotNull(
                constraints.minLength?.let { "min = $it" },
                constraints.maxLength?.let { "max = $it" },
            )
        return listOfNotNull(
            sizeArguments.takeIf { it.isNotEmpty() }?.let { annotation("Size", it.joinToString(", ")) },
            constraints.pattern?.let { annotation("Pattern", "regexp = \"${escape(it)}\"") },
            annotation("Email").takeIf { constraints.email },
        )
    }

    private fun annotation(
        name: String,
        arguments: String? = null,
    ): String {
        val target = if (sourceLanguage == SourceLanguage.KOTLIN) "@field:" else "@"
        return if (arguments == null) "$target$name" else "$target$name($arguments)"
    }

    private fun escape(value: String): String =
        when (sourceLanguage) {
            SourceLanguage.KOTLIN -> SourceLiteralEscaper.forKotlin(value)
            SourceLanguage.JAVA -> SourceLiteralEscaper.forJava(value)
        }
}
