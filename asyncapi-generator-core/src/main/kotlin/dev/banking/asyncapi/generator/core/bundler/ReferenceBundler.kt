package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.model.references.Reference

/**
 * Shared reference-bundling behavior used by object-specific bundlers.
 *
 * Object-specific bundlers should decide which fields to traverse. This helper
 * keeps the common reference handling, such as marking an unvisited reference
 * as inline, in one place. It never changes the reference it is given: bundling
 * returns a new [Reference], because the parser shares reference instances
 * between places of the document.
 */
internal object ReferenceBundler {

    /** Returns an inline copy of [reference], or [reference] itself when it is already being visited. */
    fun inlineIfUnvisited(
        reference: Reference,
        context: BundlingContext,
    ): Reference {
        if (context.hasVisited(reference)) return reference
        return reference.copy().inline()
    }

    /** Returns an inline copy of [reference] holding the bundled model, or [reference] itself when it is already being visited. */
    inline fun <reified T> bundleReferencedModel(
        reference: Reference,
        context: BundlingContext,
        bundle: (T, BundlingContext) -> T,
    ): Reference {
        if (context.hasVisited(reference)) return reference
        val model = reference.requireModel<T>()
        val bundled = bundle(model, context.enter(reference))
        return reference.copy(model = bundled).inline()
    }
}

/** A copy of this reference pointing at [model], keeping its inline state. */
internal fun Reference.withModel(model: Any?): Reference = copy(model = model).also { it.inline = inline }
