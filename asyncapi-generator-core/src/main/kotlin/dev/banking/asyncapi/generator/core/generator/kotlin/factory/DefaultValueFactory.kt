package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.model.EnumLiteralNormalizer
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.util.SourceLiteralEscaper
import dev.banking.asyncapi.generator.core.model.schemas.Schema

class DefaultValueFactory(private val context: GeneratorContext) {

    fun createDefaultValue(
        schema: Schema,
        baseKotlinType: String,
        isNullable: Boolean
    ): String? {
        if (schema.default == null) {
            return if (isNullable) "null" else null
        }

        val defaultValRaw = schema.default.toString()
        val defaultVal = defaultValRaw.trimStart('"', '\'', '|', '>').removeSurrounding("\"")

        val schemaForKotlinType = context.findSchemaByName(baseKotlinType)
        val isEnum = schemaForKotlinType?.enum?.isNotEmpty() == true

        return if (isEnum) {
            "$baseKotlinType.${EnumLiteralNormalizer.constantName(defaultVal) ?: defaultVal}"
        } else {
            when (schema.type.getPrimaryType()) {
                "boolean", "integer" -> defaultVal
                "number" -> numberLiteral(defaultVal, baseKotlinType)
                "string" -> "\"${SourceLiteralEscaper.forKotlin(defaultVal)}\""
                else -> null
            }
        }
    }

    /** A number literal of the property's Kotlin type: `1` is an Int literal, which a Double property rejects. */
    private fun numberLiteral(value: String, kotlinType: String): String {
        val decimal = if (value.contains('.') || value.contains('e', ignoreCase = true)) value else "$value.0"
        return when (kotlinType) {
            "Double" -> decimal
            "Float" -> "${decimal}f"
            "BigDecimal" -> "BigDecimal(\"$value\")"
            else -> value
        }
    }
}
