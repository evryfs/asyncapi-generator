package dev.banking.asyncapi.generator.core.validator.util

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationError
import dev.banking.asyncapi.generator.core.model.validator.ValidationWarning
import org.slf4j.LoggerFactory

class ValidationResults(
    val asyncApiContext: AsyncApiContext,
) {
    private val logger = LoggerFactory.getLogger(ValidationResults::class.java)

    private val _errors = mutableListOf<ValidationError>()
    private val _warnings = mutableListOf<ValidationWarning>()

    val errors: List<ValidationError> get() = _errors
    val warnings: List<ValidationWarning> get() = _warnings

    fun error(message: String, line: Int? = null, doc: String? = null) {
        _errors += ValidationError(message, line, doc)
    }

    fun warn(message: String, line: Int? = null, doc: String? = null) {
        _warnings += ValidationWarning(message, line, doc)
    }

    fun hasErrors() = _errors.isNotEmpty()

    fun hasWarnings() = _warnings.isNotEmpty()

    fun throwErrors() {
        if (_errors.isNotEmpty()) {
            throw AsyncApiValidateException.ValidateError(_errors, asyncApiContext)
        }
    }

    fun logWarnings() {
        if (_warnings.isNotEmpty()) {
            logger.warn(
                buildString {
                    appendLine("Validation found ${_warnings.size} warning(s):")
                    appendLine()
                    _warnings.forEach { warning ->
                        appendLine(">> ${warning.message}")
                        appendLine()
                        appendLine(asyncApiContext.validatorSnippet(warning.line ?: -1))
                        appendLine()
                        if (warning.doc != null) appendLine("See documentation: ${warning.doc}")
                        appendLine("---------------------------------------------------------------------------------------------------------------------")
                        appendLine()
                    }
                }
            )
        }
    }
}
