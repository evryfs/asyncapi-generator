package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model.KotlinFieldTemplate
import com.tietoevry.banking.asyncapi.generator.core.generator.util.TypeConstants.JAVA_BUILTINS
import com.tietoevry.banking.asyncapi.generator.core.generator.util.TypeConstants.KOTLIN_BUILTINS

class ImportMapper(private val modelPackage: String) {

    fun computeImports(
        currentClassName: String,
        fields: List<KotlinFieldTemplate>,
    ): List<String> {
        val imports = linkedSetOf<String>()

        fields.forEach { field ->
            val raw = field.type.removeSuffix("?")

            // Built-ins from java.* → add correct import
            when (raw) {
                "UUID" -> imports += "java.util.UUID"
                "LocalDate" -> imports += "java.time.LocalDate"
                "LocalTime" -> imports += "java.time.LocalTime"
                "OffsetDateTime" -> imports += "java.time.OffsetDateTime"
                "BigDecimal" -> imports += "java.math.BigDecimal"
            }

            // Special case: List<T>
            if (raw.startsWith("List<") && raw.endsWith(">")) {
                val element = raw.removePrefix("List<").removeSuffix(">")

                addIfModelType(imports, element, currentClassName)
            } else {
                addIfModelType(imports, raw, currentClassName)
            }

            // Constraint-related imports
            field.annotations.forEach { ann ->
                when {
                    ann.startsWith("@field:Size") -> imports += "jakarta.validation.constraints.Size"
                    ann.startsWith("@field:Pattern") -> imports += "jakarta.validation.constraints.Pattern"
                    ann.startsWith("@field:Min") -> imports += "jakarta.validation.constraints.Min"
                    ann.startsWith("@field:Max") -> imports += "jakarta.validation.constraints.Max"
                    ann.startsWith("@field:DecimalMin") -> imports += "jakarta.validation.constraints.DecimalMin"
                    ann.startsWith("@field:DecimalMax") -> imports += "jakarta.validation.constraints.DecimalMax"
                    ann.startsWith("@field:Email") -> imports += "jakarta.validation.constraints.Email"
                    ann.startsWith("@field:Valid") -> imports += "jakarta.validation.Valid"
                    ann.startsWith("@JsonProperty") -> {
                        imports += "com.fasterxml.jackson.annotation.JsonProperty"
                        imports += "com.fasterxml.jackson.annotation.JsonProperty.Access"
                    }
                }
            }
        }

        return imports.toList().sorted()
    }

    private fun addIfModelType(imports: MutableSet<String>, type: String, currentClassName: String) {
        val clean = type.removeSuffix("?")

        if (clean.isBlank()) return
        if (clean in KOTLIN_BUILTINS) return
        if (clean in JAVA_BUILTINS) return

        // Skip collection types
        if (clean.startsWith("List<")) return
        if (clean.startsWith("Map<")) return

        // Skip self-imports
        if (clean == currentClassName) return

        // Skip fully-qualified classes
        if (clean.contains(".")) return

        // Only UPPERCASE-leading types are model classes
        if (!clean.first().isUpperCase()) return

        // ✔ This is a real generated model type → import it
        imports += "$modelPackage.$clean"
    }
}
