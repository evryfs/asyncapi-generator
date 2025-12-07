package com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.java.model.JavaFieldTemplate
import com.tietoevry.banking.asyncapi.generator.core.generator.util.TypeConstants.JAVA_BUILTINS

class ImportMapper(private val modelPackage: String) {

    private val JAVA_LANG_BUILTINS = setOf("String", "Integer", "Long", "Double", "Float", "Boolean", "Object", "Void")

    fun computeImports(
        currentClassName: String,
        fields: List<JavaFieldTemplate>,
    ): List<String> {
        val imports = linkedSetOf<String>()

        // Standard imports for generated POJOs
        imports.add("java.util.Objects")
        imports.add("java.io.Serializable")

        fields.forEach { field ->
            val raw = field.type // e.g., "Map<String, String>", "List<SomeItem>"

            // Built-ins from java.* → add correct import
            when {
                raw == "UUID" -> imports += "java.util.UUID"
                raw == "LocalDate" -> imports += "java.time.LocalDate"
                raw == "LocalTime" -> imports += "java.time.LocalTime"
                raw == "OffsetDateTime" -> imports += "java.time.OffsetDateTime"
                raw == "BigDecimal" -> imports += "java.math.BigDecimal"
                raw.startsWith("List<") -> imports += "java.util.List"
                raw.startsWith("Map<") -> imports += "java.util.Map"
            }

            // Parse List<T> or Map<K,V> to find inner models (recursive call handles this)
            if (raw.startsWith("List<") && raw.endsWith(">")) {
                val element = raw.removePrefix("List<").removeSuffix(">").trim()
                addIfModelType(imports, element, currentClassName)
            } else if (raw.startsWith("Map<") && raw.endsWith(">")) {
                val inner = raw.removePrefix("Map<").removeSuffix(">")
                val parts = inner.split(",")
                if (parts.size == 2) {
                    addIfModelType(imports, parts[1].trim(), currentClassName)
                }
            } else {
                addIfModelType(imports, raw, currentClassName)
            }

            // Annotation Imports
            field.annotations.forEach { ann ->
                when {
                    ann.startsWith("@NotNull") -> imports += "jakarta.validation.constraints.NotNull"
                    ann.startsWith("@Size") -> imports += "jakarta.validation.constraints.Size"
                    ann.startsWith("@Pattern") -> imports += "jakarta.validation.constraints.Pattern"
                    ann.startsWith("@Min") -> imports += "jakarta.validation.constraints.Min"
                    ann.startsWith("@Max") -> imports += "jakarta.validation.constraints.Max"
                    ann.startsWith("@DecimalMin") -> imports += "jakarta.validation.constraints.DecimalMin"
                    ann.startsWith("@DecimalMax") -> imports += "jakarta.validation.constraints.DecimalMax"
                    ann.startsWith("@Email") -> imports += "jakarta.validation.constraints.Email"
                    ann.startsWith("@Valid") -> imports += "jakarta.validation.Valid"
                    ann.startsWith("@JsonProperty") -> {
                        imports += "com.fasterxml.jackson.annotation.JsonProperty"
                        // Add import for Access enum if it's used
                        if (ann.contains("access = Access.")) {
                            imports += "com.fasterxml.jackson.annotation.JsonProperty.Access"
                        }
                    }
                }
            }
        }

        return imports.toList().sorted()
    }

    private fun addIfModelType(imports: MutableSet<String>, type: String, currentClassName: String) {
        val clean = type.trim()
        if (clean.isBlank()) return
        if (clean in JAVA_LANG_BUILTINS) return
        if (clean in JAVA_BUILTINS) return
        if (clean == currentClassName) return
        if (clean.contains(".")) return
        if (!clean.first().isUpperCase()) return
        imports += "$modelPackage.$clean"
    }
}
