package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema

class DefaultValueFactory(private val context: GeneratorContext) {

    fun createDefaultValue(
        schema: Schema,
        baseJavaType: String,
        isNullable: Boolean
    ): String? {
        if (schema.default == null) {
            return if (isNullable) "null" else null
        }

        val defaultValRaw = schema.default.toString()
        val defaultVal = defaultValRaw.trimStart('"', '\'', '|', '>').removeSurrounding("\"")

        val schemaForJavaType = context.findSchemaByName(baseJavaType)
        val isEnum = schemaForJavaType?.enum?.isNotEmpty() == true

        return if (isEnum) {
            "$baseJavaType.${defaultVal.uppercase().replace("-", "_")}"
        } else {
            when (schema.type.getPrimaryType()) {
                "boolean" -> defaultVal
                "integer" -> {
                    if (baseJavaType == "Long") "${defaultVal}L" else defaultVal
                }
                "number" -> {
                    if (baseJavaType == "Float") "${defaultVal}f"
                    else if (baseJavaType == "Double") "${defaultVal}d"
                    else if (baseJavaType == "BigDecimal") "new java.math.BigDecimal(\"$defaultVal\")"
                    else defaultVal
                }
                "string" -> "\"$defaultVal\""
                else -> null
            }
        }
    }
}
