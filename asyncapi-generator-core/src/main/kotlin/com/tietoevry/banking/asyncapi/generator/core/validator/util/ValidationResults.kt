package com.tietoevry.banking.asyncapi.generator.core.validator.util

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import com.tietoevry.banking.asyncapi.generator.core.model.validator.ValidationError
import com.tietoevry.banking.asyncapi.generator.core.model.validator.ValidationWarning

class ValidationResults(
    val asyncApiContext: AsyncApiContext,
) {

    private val _errors = mutableListOf<ValidationError>()
    private val _warnings = mutableListOf<ValidationWarning>()

    val errors: List<ValidationError> get() = _errors
    val warnings: List<ValidationWarning> get() = _warnings

    fun error(message: String, line: Int? = null) {
        _errors += ValidationError(message, line)
    }

    fun warn(message: String, line: Int? = null) {
        _warnings += ValidationWarning(message, line)
    }

    fun hasErrors() = _errors.isNotEmpty()

    fun hasWarnings() = _warnings.isNotEmpty()

    fun throwErrors() {
        if (_errors.isNotEmpty()) {
            throw AsyncApiValidateException.ValidateError(_errors, asyncApiContext)
        }
    }

    fun throwWarnings() {
        if (_warnings.isNotEmpty()) {
            _warnings.forEach {
                // TODO - give warnings proper context like errors
                println("Warning: ${it.message}")
            }
        }
    }

    fun missingReference(ref: String, line: Int?) {
        error("Unresolved reference: '$ref'. The target definition was not found.", line)
    }
}
