package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.validator.ValidationConcern
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity

/**
 * Collects and deduplicates validation warnings from external document loading.
 */
internal class ValidationWarningCollector {

    private val warnings = linkedMapOf<FindingIdentity, ValidationFinding>()

    fun collect(newWarnings: List<ValidationFinding>) {
        newWarnings.forEach { warning ->
            warnings.putIfAbsent(FindingIdentity.of(warning), warning)
        }
    }

    fun mergeWith(rootWarnings: List<ValidationFinding>): List<ValidationFinding> =
        buildMap {
            putAll(warnings)
            rootWarnings.forEach { warning -> putIfAbsent(FindingIdentity.of(warning), warning) }
        }.values.toList()

    private data class FindingIdentity(
        val code: String,
        val concern: ValidationConcern,
        val severity: ValidationSeverity,
        val documentation: String,
        val file: String?,
        val path: String?,
        val line: Int?,
        val column: Int?,
    ) {
        companion object {
            fun of(finding: ValidationFinding): FindingIdentity =
                FindingIdentity(
                    code = finding.code,
                    concern = finding.concern,
                    severity = finding.severity,
                    documentation = finding.documentation,
                    file = finding.sourceLocation?.file?.canonicalPath,
                    path = finding.path,
                    line = finding.line,
                    column = finding.sourceLocation?.column,
                )
        }
    }
}
