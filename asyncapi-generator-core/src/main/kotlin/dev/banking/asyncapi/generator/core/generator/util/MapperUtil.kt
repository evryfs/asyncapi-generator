package dev.banking.asyncapi.generator.core.generator.util

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.NON_ALPHANUMERIC

object MapperUtil {
    private val acronymBoundary = Regex("([A-Z]+)([A-Z][a-z])")
    private val wordBoundary = Regex("([a-z0-9])([A-Z])")

    fun toPascalCase(raw: String): String =
        raw.split(NON_ALPHANUMERIC)
            .filter { it.isNotBlank() }
            .joinToString("") { part ->
                part.replaceFirstChar { it.uppercase() }
            }

    fun toCamelCase(raw: String): String =
        raw.split(NON_ALPHANUMERIC)
            .filter { it.isNotBlank() }
            .map { part -> if (part.all(Char::isUpperCase)) part.lowercase() else part }
            .mapIndexed { index, part ->
                if (index == 0) {
                    part.replaceFirstChar { it.lowercase() }
                } else {
                    part.replaceFirstChar { it.uppercase() }
                }
            }
            .joinToString("")

    fun toUpperSnakeCase(raw: String): String =
        raw
            .replace(acronymBoundary, "$1_$2")
            .replace(wordBoundary, "$1_$2")
            .split(NON_ALPHANUMERIC)
            .filter { it.isNotBlank() }
            .joinToString("_") { part -> part.uppercase() }

    fun Any?.getPrimaryType(): String? {
        return when (this) {
            is String -> normalizeSchemaType()
            is List<*> -> this
                .filterIsInstance<String>()
                .map { it.normalizeSchemaType() }
                .firstOrNull { !it.equals("null", ignoreCase = true) }

            else -> null
        }
    }

    fun Any?.isTypeNullable(): Boolean {
        return when (this) {
            is String -> normalizeSchemaType().equals("null", ignoreCase = true)
            is List<*> -> this
                .filterIsInstance<String>()
                .any { it.normalizeSchemaType().equals("null", ignoreCase = true) }

            else -> false
        }
    }

    fun Any?.hasMultipleNonNullTypes(): Boolean {
        val types = (this as? List<*>)?.filterIsInstance<String>()
            ?.map { it.normalizeSchemaType() }
            ?.filter { !it.equals("null", ignoreCase = true) }
            ?: return false
        return types.distinct().size > 1
    }

    private fun String.normalizeSchemaType(): String =
        trim()
            .trimStart('"', '\'', '|', '>')
            .trimEnd('"', '\'')
}
