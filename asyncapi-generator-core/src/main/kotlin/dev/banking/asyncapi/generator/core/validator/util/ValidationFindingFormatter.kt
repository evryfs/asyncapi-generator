package dev.banking.asyncapi.generator.core.validator.util

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding

/**
 * Formats validation findings for exceptions and logs.
 *
 * - validator package tests
 */
internal object ValidationFindingFormatter {
    fun format(
        title: String,
        findings: List<ValidationFinding>,
        asyncApiContext: AsyncApiContext,
    ): String =
        DiagnosticLayout.format(
            title,
            findings.map { finding ->
                DiagnosticLayout.Entry(finding.message, snippet(finding, asyncApiContext), finding.documentation)
            },
        )

    private fun snippet(
        finding: ValidationFinding,
        asyncApiContext: AsyncApiContext,
    ): String {
        finding.sourceLocation?.let { return asyncApiContext.sourceTracking.snippet(it) }
        finding.path?.let { return asyncApiContext.snippet(it) }
        return "(no source location available)"
    }
}
