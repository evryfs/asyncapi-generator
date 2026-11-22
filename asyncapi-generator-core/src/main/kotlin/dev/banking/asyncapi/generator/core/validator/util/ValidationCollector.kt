package dev.banking.asyncapi.generator.core.validator.util

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.source.SourceLocation
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import java.util.Collections
import java.util.IdentityHashMap

/** Mutable validation state confined to one validator invocation. */
internal class ValidationCollector internal constructor() {
    /**
     * A model a reference led to, still to be checked.
     *
     * @property category the kind of object the reference expects
     * @property target the model
     * @property path the reference as findings name it
     */
    internal data class PendingReferenceTarget(
        val category: ReferenceCategoryKey,
        val target: Any,
        val path: ValidationPath,
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
        path: ValidationPath,
    ) {
        pendingReferenceTargets.addLast(PendingReferenceTarget(category, target, path))
    }

    internal fun nextReferenceTarget(): PendingReferenceTarget? = pendingReferenceTargets.removeFirstOrNull()

    internal fun error(
        rule: ValidationRule,
        message: String,
        sourceLocation: SourceLocation? = null,
    ) {
        require(rule.severity == ERROR) { "Rule ${rule.code} is not an error rule" }
        add(rule, message, sourceLocation)
    }

    internal fun warn(
        rule: ValidationRule,
        message: String,
        sourceLocation: SourceLocation? = null,
    ) {
        require(rule.severity == WARNING) { "Rule ${rule.code} is not a warning rule" }
        add(rule, message, sourceLocation)
    }

    internal fun report(): ValidationReport = ValidationReport(findings)

    private fun add(
        rule: ValidationRule,
        message: String,
        sourceLocation: SourceLocation?,
    ) {
        findings +=
            ValidationFinding(
                code = rule.code,
                concern = rule.concern,
                severity = rule.severity,
                message = message,
                sourceLocation = sourceLocation,
                documentation = rule.documentation,
            )
    }
}
