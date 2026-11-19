package dev.banking.asyncapi.generator.core.diagnostics

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport
import org.slf4j.LoggerFactory

/** Formats and delivers an immutable validation report at an application boundary. */
internal class ValidationReporter(
    private val context: AsyncApiContext,
) {
    private val logger = LoggerFactory.getLogger(ValidationReporter::class.java)

    fun throwErrors(report: ValidationReport) {
        if (report.errors.isNotEmpty()) {
            throw AsyncApiValidateException.ValidateError(
                report.errors,
                ValidationFindingFormatter.format(
                    title = "Validation failed with ${report.errors.size} error(s):",
                    findings = report.errors,
                    asyncApiContext = context,
                ),
            )
        }
    }

    fun logWarnings(report: ValidationReport) {
        if (report.warnings.isNotEmpty()) {
            logger.warn(
                ValidationFindingFormatter.format(
                    title = "Validation found ${report.warnings.size} warning(s):",
                    findings = report.warnings,
                    asyncApiContext = context,
                ),
            )
        }
    }
}
