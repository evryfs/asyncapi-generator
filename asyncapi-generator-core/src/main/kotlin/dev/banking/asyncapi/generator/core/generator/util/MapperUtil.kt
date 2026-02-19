package dev.banking.asyncapi.generator.core.generator.util

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.NON_ALPHANUMERIC

object MapperUtil {

    fun toPascalCase(raw: String): String =
        raw.split(NON_ALPHANUMERIC)
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
