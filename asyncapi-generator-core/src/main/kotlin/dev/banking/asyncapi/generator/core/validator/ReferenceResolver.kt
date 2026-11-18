package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_CATEGORY_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_CYCLE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_TARGET_CATEGORY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_UNRESOLVED
import dev.banking.asyncapi.generator.core.parser.linking.ReferenceLink
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

/**
 * Reports where a reference leads and queues its target for validation.
 *
 * The reference's link comes from the load's [dev.banking.asyncapi.generator.core.parser.linking.ReferenceLinker];
 * this class only turns its problems into findings.
 *
 * @param asyncApiContext the load the references belong to
 */
internal class ReferenceResolver(
    private val asyncApiContext: AsyncApiContext,
) {
    /**
     * Reports the problems of a reference and returns its target.
     *
     * A reference already reported in [results] is not reported again.
     *
     * @param reference the reference
     * @param expectedCategory the kind of object the reference must point at
     * @param contextString the object that holds the reference, as named in findings
     * @param results the collector findings are reported to
     * @param targetCategoryRule the rule reported when the reference reaches another kind of object
     * @return the model the reference leads to, or `null` when it leads to none
     */
    fun resolve(
        reference: Reference,
        expectedCategory: ReferenceCategoryKey,
        contextString: String,
        results: ValidationCollector,
        targetCategoryRule: ValidationRule = REFERENCE_TARGET_CATEGORY,
    ): Any? {
        if (reference.referenceCategoryKey != expectedCategory) {
            results.error(
                REFERENCE_CATEGORY_REQUIRED,
                "$contextString reference '${reference.ref}' was created for category " +
                    "'${reference.referenceCategoryKey}' instead of '$expectedCategory'.",
                sourceLocation = location(reference),
            )
            return null
        }
        val link = asyncApiContext.referenceLinker.link(reference)
        if (!results.process(reference)) return link.finalModel()
        link.chain.drop(1).forEach(results::process)
        return when (link) {
            is ReferenceLink.Resolved ->
                link.target.also {
                    results.enqueueReferenceTarget(
                        expectedCategory,
                        it,
                        contextString,
                    )
                }
            is ReferenceLink.Unresolved -> reportUnresolved(link, contextString, results)
            is ReferenceLink.Cycle -> reportCycle(link, expectedCategory, contextString, results)
            is ReferenceLink.CategoryMismatch -> reportCategoryMismatch(link, expectedCategory, contextString, results)
            is ReferenceLink.WrongTarget -> {
                results.error(
                    targetCategoryRule,
                    "$contextString reference '${reference.ref}' must resolve to ${expectedCategory.displayName}; " +
                        "found ${link.target::class.simpleName}.",
                    sourceLocation = location(reference),
                )
                null
            }
        }
    }

    private fun reportUnresolved(
        link: ReferenceLink.Unresolved,
        contextString: String,
        results: ValidationCollector,
    ): Any? {
        val unresolved = link.chain.last()
        results.error(
            REFERENCE_UNRESOLVED,
            "$contextString reference '${unresolved.ref}' could not be resolved",
            sourceLocation = location(unresolved),
        )
        return null
    }

    private fun reportCycle(
        link: ReferenceLink.Cycle,
        expectedCategory: ReferenceCategoryKey,
        contextString: String,
        results: ValidationCollector,
    ): Any? {
        results.process(link.target)
        val reference = link.chain.first()
        val cycleStart = link.chain.indexOfFirst { it === link.target }
        val cycle = (link.chain.drop(cycleStart).map { it.ref } + link.target.ref).joinToString(" -> ")
        results.error(
            REFERENCE_CYCLE,
            "$contextString reference '${reference.ref}' never reaches ${expectedCategory.displayName}: " +
                "its references form a cycle ($cycle).",
            sourceLocation = location(reference),
        )
        return null
    }

    private fun reportCategoryMismatch(
        link: ReferenceLink.CategoryMismatch,
        expectedCategory: ReferenceCategoryKey,
        contextString: String,
        results: ValidationCollector,
    ): Any? {
        results.process(link.target)
        results.error(
            REFERENCE_CATEGORY_REQUIRED,
            "$contextString reference '${link.target.ref}' was created for category " +
                "'${link.target.referenceCategoryKey}' instead of '$expectedCategory'.",
            sourceLocation = location(link.target),
        )
        return null
    }

    private fun location(reference: Reference) =
        asyncApiContext.modelTracking.getSourceLocation(reference, reference::ref)

    /** The model at the end of the chain, of whatever kind, or `null` when the chain reaches none. */
    private fun ReferenceLink.finalModel(): Any? =
        when (this) {
            is ReferenceLink.Resolved -> target
            is ReferenceLink.WrongTarget -> target
            else -> null
        }
}
