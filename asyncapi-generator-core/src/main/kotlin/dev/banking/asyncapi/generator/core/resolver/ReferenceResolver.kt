package dev.banking.asyncapi.generator.core.resolver

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_CATEGORY_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_CYCLE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_TARGET_CATEGORY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_UNRESOLVED
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Resolves references and validates they point to the correct target type.
 *
 * Follows reference chains, checks category consistency, and reports unresolved or mismatched targets.
 */
internal class ReferenceResolver(
    private val asyncApiContext: AsyncApiContext,
) {
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
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(reference, reference::ref),
            )
            return null
        }
        if (!results.process(reference)) {
            return finalTarget(reference)
        }

        val chain: MutableSet<Reference> =
            Collections.newSetFromMap(IdentityHashMap())
        val path = mutableListOf<Reference>()
        var current = reference
        while (chain.add(current)) {
            path += current
            val target = asyncApiContext.modelTracking.findReference(current)
            if (target == null) {
                results.error(
                    REFERENCE_UNRESOLVED,
                    "$contextString reference '${current.ref}' could not be resolved",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(current, current::ref),
                )
                return null
            }
            current.model = target
            if (target is Reference) {
                results.process(target)
                if (target in chain) {
                    val cycleStart = path.indexOfFirst { it === target }
                    val cycle = (path.drop(cycleStart).map { it.ref } + target.ref).joinToString(" -> ")
                    results.error(
                        REFERENCE_CYCLE,
                        "$contextString reference '${reference.ref}' never reaches ${expectedCategory.displayName}: " +
                            "its references form a cycle ($cycle).",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(reference, reference::ref),
                    )
                    return null
                }
                if (target.referenceCategoryKey != expectedCategory) {
                    results.error(
                        REFERENCE_CATEGORY_REQUIRED,
                        "$contextString reference '${target.ref}' was created for category " +
                            "'${target.referenceCategoryKey}' instead of '$expectedCategory'.",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(target, target::ref),
                    )
                    return null
                }
                current = target
                continue
            }
            if (!expectedCategory.accepts(target)) {
                results.error(
                    targetCategoryRule,
                    "$contextString reference '${reference.ref}' must resolve to ${expectedCategory.displayName}; " +
                        "found ${target::class.simpleName}.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(reference, reference::ref),
                )
                return null
            }
            results.enqueueReferenceTarget(expectedCategory, target, contextString)
            return target
        }

        return null
    }

    private fun finalTarget(reference: Reference): Any? {
        val chain: MutableSet<Reference> =
            Collections.newSetFromMap(IdentityHashMap())
        var target: Any? = reference
        while (target is Reference && chain.add(target)) {
            target = target.model
        }
        return target?.takeUnless { it is Reference }
    }
}
