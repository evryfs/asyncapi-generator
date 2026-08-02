package dev.banking.asyncapi.generator.core.model.validator

/**
 * Severity for a validation finding.
 *
 * Expected behavior is covered by:
 * - `ValidationReportTest`
 */
enum class ValidationSeverity {
    ERROR,
    WARNING,
}
