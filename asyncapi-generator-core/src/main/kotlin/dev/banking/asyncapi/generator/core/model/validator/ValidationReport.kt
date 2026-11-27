package dev.banking.asyncapi.generator.core.model.validator

import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import java.util.Collections.unmodifiableList

/**
 * The findings of validating one document, in document order.
 *
 * @param findings the findings
 */
internal class ValidationReport internal constructor(
    findings: List<ValidationFinding>,
) {
    val findings: List<ValidationFinding> = unmodifiableList(findings.toList())
    val errors: List<ValidationFinding> =
        unmodifiableList(this.findings.filter { it.severity == ERROR })
    val warnings: List<ValidationFinding> =
        unmodifiableList(this.findings.filter { it.severity == WARNING })

    fun hasErrors(): Boolean = errors.isNotEmpty()

    fun hasWarnings(): Boolean = warnings.isNotEmpty()
}
