package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
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
            "$baseKotlinType.${defaultVal.uppercase().replace("-", "_")}"
        } else {
            when (schema.type.getPrimaryType()) {
                "boolean", "integer", "number" -> defaultVal
                "string" -> "\"$defaultVal\""
                else -> null
            }
        }
    }
}
