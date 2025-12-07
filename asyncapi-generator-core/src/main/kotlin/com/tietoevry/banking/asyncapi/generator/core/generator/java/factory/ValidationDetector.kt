package com.tietoevry.banking.asyncapi.generator.core.generator.java.factory

import com.tietoevry.banking.asyncapi.generator.core.generator.context.GeneratorContext
import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import com.tietoevry.banking.asyncapi.generator.core.generator.util.TypeConstants.JAVA_BUILTINS

class ValidationDetector(private val context: GeneratorContext) {

    // Basic types that don't need @Valid
    private val JAVA_PRIMITIVES = setOf("String", "Integer", "Long", "Double", "Float", "Boolean", "Object") + JAVA_BUILTINS

    fun needsCascadedValidation(javaType: String): Boolean {
        val raw = javaType.trim()

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
        val clean = typeName.trim()
        if (clean in JAVA_PRIMITIVES) return false

        val schema = context.findSchemaByName(clean)
        return if (schema != null) {
            val isObjectType = schema.type.getPrimaryType() == "object"
            val isPolymorphicType = !schema.oneOf.isNullOrEmpty() || !schema.anyOf.isNullOrEmpty()
            isObjectType || isPolymorphicType
        } else {
            clean.firstOrNull()?.isUpperCase() == true
        }
    }
}
