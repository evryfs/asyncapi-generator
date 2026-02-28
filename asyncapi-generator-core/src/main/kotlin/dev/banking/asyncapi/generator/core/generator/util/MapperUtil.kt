package dev.banking.asyncapi.generator.core.generator.util

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.NON_ALPHANUMERIC
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility

object MapperUtil {

    fun toPascalCase(raw: String): String =
        raw.split(NON_ALPHANUMERIC)
            .filter { it.isNotBlank() }
            .joinToString("") { part ->
                part.replaceFirstChar { it.uppercase() }
            }

    fun Any?.getPrimaryType(): String? {
        return when (this) {
            is String -> ValidatorUtility.sanitizeString(this)
            is List<*> -> this
                .filterIsInstance<String>()
                .map { ValidatorUtility.sanitizeString(it) }
                .firstOrNull { !it.equals("null", ignoreCase = true) }

            else -> null
        }
    }

    fun Any?.isTypeNullable(): Boolean {
        return when (this) {
            is String -> ValidatorUtility.sanitizeString(this).equals("null", ignoreCase = true)
            is List<*> -> this
                .filterIsInstance<String>()
                .any { ValidatorUtility.sanitizeString(it).equals("null", ignoreCase = true) }

            else -> false
        }
    }

    fun Any?.hasMultipleNonNullTypes(): Boolean {
        val types = (this as? List<*>)?.filterIsInstance<String>()
            ?.map { ValidatorUtility.sanitizeString(it) }
            ?.filter { !it.equals("null", ignoreCase = true) }
            ?: return false
        return types.distinct().size > 1
    }

}
