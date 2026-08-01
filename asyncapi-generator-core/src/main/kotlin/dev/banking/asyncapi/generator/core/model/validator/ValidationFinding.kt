package dev.banking.asyncapi.generator.core.model.validator

import dev.banking.asyncapi.generator.core.document.SourceLocation

/**
 * Structured validation diagnostic produced by the validator stage.
 *
 * Expected behavior is covered by:
 * - `ValidationResultsTest`
 */
data class ValidationFinding(
    val severity: ValidationSeverity,
    val message: String,
    val sourceLocation: SourceLocation? = null,
    val path: String? = sourceLocation?.path,
    val line: Int? = sourceLocation?.line,
    val doc: String? = null,
)
