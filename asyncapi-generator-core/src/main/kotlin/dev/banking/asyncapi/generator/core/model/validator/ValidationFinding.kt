package dev.banking.asyncapi.generator.core.model.validator

import dev.banking.asyncapi.generator.core.source.SourceLocation

/**
 * One validation finding.
 *
 * @property code the code of the rule that produced it
 * @property concern who owns the rule
 * @property severity whether it fails the load
 * @property message what is wrong, starting with the object it is about
 * @property sourceLocation where in the source it is, when known
 * @property documentation where the rule is documented
 */
internal data class ValidationFinding(
    val code: String,
    val concern: ValidationConcern,
    val severity: ValidationSeverity,
    val message: String,
    val sourceLocation: SourceLocation? = null,
    val documentation: String,
) {
    val path: String? get() = sourceLocation?.path
    val line: Int? get() = sourceLocation?.line
}
