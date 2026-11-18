package dev.banking.asyncapi.generator.core.diagnostics

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding

/** Formats validation findings, each with a snippet of its source, for exceptions and logs. */
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
    ): String = finding.sourceLocation?.let(asyncApiContext.sourceTracking::snippet) ?: "(no source location available)"
}
