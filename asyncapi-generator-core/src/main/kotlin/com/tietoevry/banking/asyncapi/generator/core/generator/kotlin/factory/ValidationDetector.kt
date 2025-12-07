package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.factory

import com.tietoevry.banking.asyncapi.generator.core.generator.context.GeneratorContext
import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import com.tietoevry.banking.asyncapi.generator.core.generator.util.TypeConstants.JAVA_BUILTINS
import com.tietoevry.banking.asyncapi.generator.core.generator.util.TypeConstants.KOTLIN_BUILTINS

class ValidationDetector(private val context: GeneratorContext) {

    fun needsCascadedValidation(kotlinType: String): Boolean {
        val raw = kotlinType.removeSuffix("?").trim()

        if (raw.startsWith("List<") && raw.endsWith(">")) {
            val element = raw.removePrefix("List<").removeSuffix(">").trim()
            return isModelType(element)
        }
        if (raw.startsWith("Map<") && raw.endsWith(">")) {
            val inner = raw.removePrefix("Map<").removeSuffix(">")
            val parts = inner.split(",")
            if (parts.size == 2) {
                return isModelType(parts[1].trim())
            }
            return false
        }
        return isModelType(raw)
    }

    private fun isModelType(typeName: String): Boolean {
        val clean = typeName.removeSuffix("?").trim()
        if (clean in KOTLIN_BUILTINS) return false
        if (clean in JAVA_BUILTINS) return false

        val schema = context.findSchemaByName(clean)
        return if (schema != null) {
            val isObjectType = schema.type.getPrimaryType() == "object"
            val isPolymorphicType = !schema.oneOf.isNullOrEmpty() || !schema.anyOf.isNullOrEmpty()
            isObjectType || isPolymorphicType
        } else {
            // Fallback: Assume UpperCamelCase starting strings are generated models
            clean.firstOrNull()?.isUpperCase() == true
        }
    }
}
