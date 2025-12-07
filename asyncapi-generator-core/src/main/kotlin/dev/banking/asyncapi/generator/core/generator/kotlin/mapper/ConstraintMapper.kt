package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema

class ConstraintMapper {

    fun buildAnnotations(schema: Schema?): List<String> {
        if (schema == null) return emptyList()

        val annotations = mutableListOf<String>()
        val type = schema.type.getPrimaryType()
        val format = schema.format

        // String constraints
        if (type == "string") {
            val minLen = schema.minLength?.toInt()
            val maxLen = schema.maxLength?.toInt()
            if (minLen != null || maxLen != null) {
                val args = mutableListOf<String>()
                if (minLen != null) args.add("min = $minLen")
                if (maxLen != null) args.add("max = $maxLen")
                annotations += "@field:Size(${args.joinToString(", ")})"
            }
            schema.pattern
                ?.trim()
                ?.removePrefix("\"")
                ?.let { annotations += "@field:Pattern(regexp = \"$it\")" }

            if (format == "email") {
                annotations += "@field:Email"
            }
        }

        // Integer constraints
        if (type == "integer") {
            val minimum = schema.minimum?.toLong()
            val maximum = schema.maximum?.toLong()
            if (minimum != null) {
                annotations += "@field:Min(${minimum}L)"
            }
            if (maximum != null) {
                annotations += "@field:Max(${maximum}L)"
            }
        }

        // Number / BigDecimal constraints
        if (type == "number") {
            val minimum = schema.minimum
            val exclusiveMin = schema.exclusiveMinimum
            val maximum = schema.maximum
            val exclusiveMax = schema.exclusiveMaximum

            if (exclusiveMin != null) {
                annotations += "@field:DecimalMin(value = \"$exclusiveMin\", inclusive = false)"
            } else if (minimum != null) {
                annotations += "@field:DecimalMin(value = \"$minimum\", inclusive = true)"
            }

            if (exclusiveMax != null) {
                annotations += "@field:DecimalMax(value = \"$exclusiveMax\", inclusive = false)"
            } else if (maximum != null) {
                annotations += "@field:DecimalMax(value = \"$maximum\", inclusive = true)"
            }
        }

        return annotations
    }
}
