package dev.banking.asyncapi.generator.core.validator.util

import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import java.util.Collections
import java.util.IdentityHashMap

/** Mutable validation state confined to one validator invocation. */
class ValidationCollector internal constructor() {
    internal data class PendingReferenceTarget(
        val category: ReferenceCategoryKey,
        val target: Any,
        val contextString: String,
    )

    private val findings = mutableListOf<ValidationFinding>()
    private val visitedModels: MutableSet<Any> =
        Collections.newSetFromMap(IdentityHashMap())
    private val processedReferences: MutableSet<Reference> =
        Collections.newSetFromMap(IdentityHashMap())
    private val pendingReferenceTargets = ArrayDeque<PendingReferenceTarget>()

    internal fun visit(model: Any): Boolean = visitedModels.add(model)

    internal fun process(reference: Reference): Boolean = processedReferences.add(reference)

    internal fun enqueueReferenceTarget(
        category: ReferenceCategoryKey,
        target: Any,
        contextString: String,
    ) {
        pendingReferenceTargets.addLast(PendingReferenceTarget(category, target, contextString))
    }

    internal fun nextReferenceTarget(): PendingReferenceTarget? =
        pendingReferenceTargets.removeFirstOrNull()

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
