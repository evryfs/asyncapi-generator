package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.util.SourceLiteralEscaper
import dev.banking.asyncapi.generator.core.model.schemas.Schema

internal class ConstraintAnnotationMapper(
    private val sourceLanguage: SourceLanguage,
) {
    fun buildAnnotations(schema: Schema?): List<String> {
        if (schema == null) return emptyList()

        val annotations = mutableListOf<String>()
        when (schema.type.getPrimaryType()) {
            "string" -> {
                val sizeArguments =
                    listOfNotNull(
                        schema.minLength?.toInt()?.let { "min = $it" },
                        schema.maxLength?.toInt()?.let { "max = $it" },
                    )
                if (sizeArguments.isNotEmpty()) {
                    annotations += annotation("Size", sizeArguments.joinToString(", "))
                }
                schema.pattern?.let { pattern ->
                    annotations += annotation("Pattern", "regexp = \"${escape(pattern)}\"")
                }
                if (schema.format == "email") {
                    annotations += annotation("Email")
                }
            }
            "integer" -> {
                schema.minimum?.toLong()?.let { annotations += annotation("Min", "${it}L") }
                schema.maximum?.toLong()?.let { annotations += annotation("Max", "${it}L") }
            }
            "number" -> {
                val minimum = schema.exclusiveMinimum ?: schema.minimum
                val maximum = schema.exclusiveMaximum ?: schema.maximum
                minimum?.let {
                    annotations += annotation(
                        "DecimalMin",
                        "value = \"$it\", inclusive = ${schema.exclusiveMinimum == null}",
                    )
                }
                maximum?.let {
                    annotations += annotation(
                        "DecimalMax",
                        "value = \"$it\", inclusive = ${schema.exclusiveMaximum == null}",
                    )
                }
            }
        }
        return annotations
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
