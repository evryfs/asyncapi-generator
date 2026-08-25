package dev.banking.asyncapi.generator.core.model.validator

import dev.banking.asyncapi.generator.core.document.SourceLocation

/**
 * Structured validation diagnostic produced by the validator stage.
 */
data class ValidationFinding(
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
