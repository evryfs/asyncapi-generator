package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.model.references.Reference

/**
 * Shared reference-bundling behavior used by object-specific bundlers.
 *
 * Object-specific bundlers should decide which fields to traverse. This helper
 * keeps common reference state changes, such as marking an unvisited reference
 * as inline, in one place.
 */
internal object ReferenceBundler {

    fun inlineIfUnvisited(
        reference: Reference,
        context: BundlingContext,
    ): Reference {
        if (!context.hasVisited(reference)) {
            reference.inline()
        }
        return reference
    }

    inline fun <reified T> bundleReferencedModel(
        reference: Reference,
        context: BundlingContext,
        bundle: (T, BundlingContext) -> T,
    ): Reference {
        if (!context.hasVisited(reference)) {
            val model = reference.requireModel<T>()
            val bundled = bundle(model, context.enter(reference))
            reference.model = bundled
            reference.inline()
        }
        return reference
    }
}
