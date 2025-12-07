package com.tietoevry.banking.asyncapi.generator.core.validator.util

object ValidatorUtility {

    fun sanitizeString(value: String): String {
        return sanitize(value) as String
    }

    fun sanitizeAny(value: Any): Any {
        return sanitize(value)
    }

    private fun sanitize(value: Any): Any {
        if (value is String) {
            return value
                .trim()
                .trimStart('"', '\'', '|', '>')
                .trimEnd('"', '\'')
        }
        return value
    }
}
