package dev.banking.asyncapi.generator.core.generator.java.mapper

import dev.banking.asyncapi.generator.core.generator.java.model.PropertyModel
import dev.banking.asyncapi.generator.core.generator.util.TypeConstants.JAVA_BUILTINS

class ImportMapper(
    val modelPackage: String,
) {

    private val JAVA_LANG_BUILTINS = setOf("String", "Integer", "Long", "Double", "Float", "Boolean", "Object", "Void")

    fun computeImports(
        currentClassName: String,
        fields: List<PropertyModel>,
    ): List<String> {
        val imports = linkedSetOf<String>()

        imports.add("java.util.Objects")
        imports.add("java.io.Serializable")

        fields.forEach { field ->
            val raw = field.typeName
            collectImports(raw, currentClassName, imports)

            field.annotations.forEach { ann ->
                val trimmedAnn = ann.trim()
                when {
                    trimmedAnn.startsWith("@NotNull") -> imports += "jakarta.validation.constraints.NotNull"
                    trimmedAnn.startsWith("@Size") -> imports += "jakarta.validation.constraints.Size"
                    trimmedAnn.startsWith("@Pattern") -> imports += "jakarta.validation.constraints.Pattern"
                    trimmedAnn.startsWith("@Min") -> imports += "jakarta.validation.constraints.Min"
                    trimmedAnn.startsWith("@Max") -> imports += "jakarta.validation.constraints.Max"
                    trimmedAnn.startsWith("@DecimalMin") -> imports += "jakarta.validation.constraints.DecimalMin"
                    trimmedAnn.startsWith("@DecimalMax") -> imports += "jakarta.validation.constraints.DecimalMax"
                    trimmedAnn.startsWith("@Email") -> imports += "jakarta.validation.constraints.Email"
                    trimmedAnn.startsWith("@Valid") -> imports += "jakarta.validation.Valid"
                    trimmedAnn.startsWith("@JsonProperty") -> {
                        imports += "com.fasterxml.jackson.annotation.JsonProperty"
                        // Add import for Access enum if it's used
                        if (trimmedAnn.contains("access = Access.")) {
                            imports += "com.fasterxml.jackson.annotation.JsonProperty.Access"
                        }
                    }
                }
            }
        }

        return imports.toList().sorted()
    }

    private fun collectImports(type: String, currentClassName: String, imports: MutableSet<String>) {
        val raw = type.trim()
        when (raw) {
            "UUID" -> {
                imports += "java.util.UUID"; return
            }
            "LocalDate" -> {
                imports += "java.time.LocalDate"; return
            }
            "LocalTime" -> {
                imports += "java.time.LocalTime"; return
            }
            "OffsetDateTime" -> {
                imports += "java.time.OffsetDateTime"; return
            }
            "BigDecimal" -> {
                imports += "java.math.BigDecimal"; return
            }
        }
        if (raw.startsWith("List<") && raw.endsWith(">")) {
            imports += "java.util.List"
            val element = raw.removePrefix("List<").removeSuffix(">")
            collectImports(element, currentClassName, imports)
            return
        }
        if (raw.startsWith("Map<") && raw.endsWith(">")) {
            imports += "java.util.Map"
            val content = raw.removePrefix("Map<").removeSuffix(">")
            val parts = content.split(",")
            if (parts.size >= 2) {
                parts.forEach { part ->
                    collectImports(part, currentClassName, imports)
                }
            }
            return
        }
        addIfModelType(imports, raw, currentClassName)
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
