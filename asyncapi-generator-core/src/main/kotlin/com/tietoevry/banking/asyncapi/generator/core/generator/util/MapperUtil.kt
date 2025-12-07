package com.tietoevry.banking.asyncapi.generator.core.generator.util

object MapperUtil {

    fun toPascalCase(raw: String): String =
        raw.split(Regex("[^A-Za-z0-9]"))
            .filter { it.isNotBlank() }
            .joinToString("") { part ->
                part.replaceFirstChar { it.uppercase() }
            }

    fun Any?.getPrimaryType(): String? {
        return when (this) {
            is String -> this
            is List<*> -> this.firstOrNull { it is String && it != "null" } as? String
            else -> null
        }
    }

    fun Any?.isTypeNullable(): Boolean {
        return this is List<*> && this.any { it == "null" }
    }
}
