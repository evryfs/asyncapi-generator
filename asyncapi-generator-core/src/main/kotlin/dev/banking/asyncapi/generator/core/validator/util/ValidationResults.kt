package dev.banking.asyncapi.generator.core.validator.util

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.validator.util.ValidationFindingFormatter.format
import org.slf4j.LoggerFactory

/**
 * Collects validation findings produced by the validator stage.
 *
 * Expected behavior is covered by:
 * - `ValidationResultsTest`
 * - validator package tests
 */
class ValidationResults(
    val asyncApiContext: AsyncApiContext,
) {
    private val logger = LoggerFactory.getLogger(ValidationResults::class.java)

    private val _findings = mutableListOf<ValidationFinding>()

    val errors: List<ValidationFinding> get() = _findings.filter { it.severity == ERROR }
    val warnings: List<ValidationFinding> get() = _findings.filter { it.severity == WARNING }
    val findings: List<ValidationFinding> get() = _findings

    fun error(
        message: String,
        line: Int? = null,
        doc: String? = null,
        sourceLocation: SourceLocation? = null,
        path: String? = sourceLocation?.path,
    ) {
        val finding = ValidationFinding(
            severity = ERROR,
            message = message,
            sourceLocation = sourceLocation,
            path = path,
            line = line ?: sourceLocation?.line,
            doc = doc,
        )
        _findings += finding
    }

    fun warn(
        message: String,
        line: Int? = null,
        doc: String? = null,
        sourceLocation: SourceLocation? = null,
        path: String? = sourceLocation?.path,
    ) {
        val finding = ValidationFinding(
            severity = WARNING,
            message = message,
            sourceLocation = sourceLocation,
            path = path,
            line = line ?: sourceLocation?.line,
            doc = doc,
        )
        _findings += finding
    }

    fun hasErrors() = errors.isNotEmpty()

    fun hasWarnings() = warnings.isNotEmpty()

    fun throwErrors() {
        val errors = errors
        if (errors.isNotEmpty()) {
            throw AsyncApiValidateException.ValidateError(errors, asyncApiContext)
        }
    }

    fun logWarnings() {
        val warnings = warnings
        if (warnings.isNotEmpty()) {
            logger.warn(
                format(
                    title = "Validation found ${warnings.size} warning(s):",
                    findings = warnings,
                    asyncApiContext = asyncApiContext,
                )
            )
        }
    }
}
