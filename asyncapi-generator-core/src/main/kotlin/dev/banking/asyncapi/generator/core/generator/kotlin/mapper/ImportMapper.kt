package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.kotlin.model.PropertyModel
import dev.banking.asyncapi.generator.core.generator.util.TypeConstants.JAVA_BUILTINS
import dev.banking.asyncapi.generator.core.generator.util.TypeConstants.KOTLIN_BUILTINS

class ImportMapper(
    val modelPackage: String,
) {

    fun computeImports(
        currentClassName: String,
        fields: List<PropertyModel>,
    ): List<String> {
        val imports = linkedSetOf<String>()

        fields.forEach { field ->
            val raw = field.baseType
            collectImports(raw, currentClassName, imports)

            field.annotations.forEach { ann ->
                val trimmedAnn = ann.trim()
                when {
                    trimmedAnn.startsWith("@field:Size") -> imports += "jakarta.validation.constraints.Size"
                    trimmedAnn.startsWith("@field:Pattern") -> imports += "jakarta.validation.constraints.Pattern"
                    trimmedAnn.startsWith("@field:Min") -> imports += "jakarta.validation.constraints.Min"
                    trimmedAnn.startsWith("@field:Max") -> imports += "jakarta.validation.constraints.Max"
                    trimmedAnn.startsWith("@field:DecimalMin") -> imports += "jakarta.validation.constraints.DecimalMin"
                    trimmedAnn.startsWith("@field:DecimalMax") -> imports += "jakarta.validation.constraints.DecimalMax"
                    trimmedAnn.startsWith("@field:Email") -> imports += "jakarta.validation.constraints.Email"
                    trimmedAnn.startsWith("@field:Valid") -> imports += "jakarta.validation.Valid"
                    trimmedAnn.startsWith("@JsonProperty") -> {
                        imports += "com.fasterxml.jackson.annotation.JsonProperty"
                        imports += "com.fasterxml.jackson.annotation.JsonProperty.Access"
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
            val element = raw.removePrefix("List<").removeSuffix(">")
            collectImports(element, currentClassName, imports) // Recurse!
            return
        }
        if (raw.startsWith("Map<") && raw.endsWith(">")) {
            val content = raw.removePrefix("Map<").removeSuffix(">")
            val parts = content.split(",")
            if (parts.size >= 2) {
                parts.forEach { part ->
                    collectImports(part, currentClassName, imports) // Recurse!
                }
            }
            return
        }
        addIfModelType(imports, raw, currentClassName)
    }

    private fun addIfModelType(imports: MutableSet<String>, type: String, currentClassName: String) {
        val clean = type.trim().removeSuffix("?")

        if (clean.isBlank()) return
        if (clean in KOTLIN_BUILTINS) return
        if (clean in JAVA_BUILTINS) return

        if (clean.startsWith("List<")) return
        if (clean.startsWith("Map<")) return

        if (clean == currentClassName) return

        if (clean.contains(".")) return

        if (!clean.first().isUpperCase()) return

        imports += "$modelPackage.$clean"
    }
}
