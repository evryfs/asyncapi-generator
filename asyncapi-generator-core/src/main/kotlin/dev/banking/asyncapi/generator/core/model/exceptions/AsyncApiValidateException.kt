package dev.banking.asyncapi.generator.core.model.exceptions

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.validator.ValidationError

sealed class AsyncApiValidateException(message: String) : Exception(message) {

    class ValidateError(
        val errors: List<ValidationError>,
        val context: AsyncApiContext
    ) : AsyncApiValidateException(
        buildString {
            appendLine("Schema validation failed with ${errors.size} error(s):")
            appendLine()
            errors.forEach { err ->
                appendLine(">> ${err.message}")
                appendLine()
                appendLine(context.validatorSnippet(err.line ?: -1))
                appendLine()
            }
        }
    )
}
