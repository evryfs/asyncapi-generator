package dev.banking.asyncapi.generator.core.parser.linking

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.parser.tracking.ModelTracking
import java.util.IdentityHashMap

/**
 * Links references to the models they point at.
 *
 * Linking a reference sets [Reference.model] on every reference of its chain to the model or reference
 * that reference points at, and records where the chain led. Each reference is followed once.
 *
 * @param modelTracking the registered models references are looked up in
 */
internal class ReferenceLinker(
    private val modelTracking: ModelTracking,
) {
    private val links = IdentityHashMap<Reference, ReferenceLink>()

    /** Links every reference in [references]. */
    fun linkAll(references: List<Reference>) {
        references.forEach(::link)
    }

    /** Links [reference], once, and returns where it led. */
    fun link(reference: Reference): ReferenceLink =
        links.getOrPut(reference) { follow(listOf(reference), reference.referenceCategoryKey) }

    private fun follow(
        chain: List<Reference>,
        category: ReferenceCategoryKey?,
    ): ReferenceLink {
        val current = chain.last()
        val target = modelTracking.findReference(current) ?: return ReferenceLink.Unresolved(chain)
        current.model = target
        return when {
            target !is Reference ->
                if (category?.accepts(target) == true) {
                    ReferenceLink.Resolved(chain, target)
                } else {
                    ReferenceLink.WrongTarget(chain, target)
                }
            chain.any { followed -> followed === target } -> ReferenceLink.Cycle(chain, target)
            target.referenceCategoryKey != category -> ReferenceLink.CategoryMismatch(chain, target)
            else -> follow(chain + target, category)
        }
    }
}
