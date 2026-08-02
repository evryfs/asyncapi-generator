package dev.banking.asyncapi.generator.core.validator.util

import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidationProfile

/** Mutable validation state confined to one validator invocation. */
class ValidationCollector internal constructor(
    internal val profile: AsyncApiValidationProfile,
) {
    private val findings = mutableListOf<ValidationFinding>()

    internal fun error(
        rule: ValidationRule,
        message: String,
        sourceLocation: SourceLocation? = null,
        doc: String? = rule.documentation,
    ) {
        require(rule.severity == ERROR) { "Rule ${rule.code} is not an error rule" }
        add(rule, message, sourceLocation, doc ?: rule.documentation)
    }

    internal fun warn(
        rule: ValidationRule,
        message: String,
        sourceLocation: SourceLocation? = null,
        doc: String? = rule.documentation,
    ) {
        require(rule.severity == WARNING) { "Rule ${rule.code} is not a warning rule" }
        add(rule, message, sourceLocation, doc ?: rule.documentation)
    }

    internal fun report(): ValidationReport = ValidationReport(findings)

    private fun add(
        rule: ValidationRule,
        message: String,
        sourceLocation: SourceLocation?,
        documentation: String,
    ) {
        findings += ValidationFinding(
            code = rule.code,
            concern = rule.concern,
            severity = rule.severity,
            message = message,
            sourceLocation = sourceLocation,
            documentation = documentation,
        )
    }
}
