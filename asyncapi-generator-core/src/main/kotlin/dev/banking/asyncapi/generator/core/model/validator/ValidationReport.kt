package dev.banking.asyncapi.generator.core.model.validator

import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import java.util.Collections.unmodifiableList

/** Immutable result of validating one parsed AsyncAPI model graph. */
class ValidationReport internal constructor(findings: List<ValidationFinding>) {
    val findings: List<ValidationFinding> = unmodifiableList(findings.toList())
    val errors: List<ValidationFinding> =
        unmodifiableList(this.findings.filter { it.severity == ERROR })
    val warnings: List<ValidationFinding> =
        unmodifiableList(this.findings.filter { it.severity == WARNING })

    fun hasErrors(): Boolean = errors.isNotEmpty()

    fun hasWarnings(): Boolean = warnings.isNotEmpty()
}
