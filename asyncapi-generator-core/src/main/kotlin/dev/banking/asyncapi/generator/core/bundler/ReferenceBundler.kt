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
    /**
     * Returns an inline copy of [reference] holding its fully resolved model, or [reference] itself when it
     * is already being visited. A reference to a component that is itself a reference is followed to the end,
     * so no inner `$ref` is left in the bundle.
     */
    fun inlineIfUnvisited(
        reference: Reference,
        scope: BundlingScope,
    ): Reference {
        if (scope.hasVisited(reference)) return reference
        return reference.copy(model = reference.requireModel<Any?>()).inline()
    }

    /** Returns an inline copy of [reference] holding the bundled model, or [reference] itself when it is already being visited. */
    inline fun <reified T> bundleReferencedModel(
        reference: Reference,
        scope: BundlingScope,
        bundle: (T, BundlingScope) -> T,
    ): Reference {
        if (scope.hasVisited(reference)) return reference
        val model = reference.requireModel<T>()
        val bundled = bundle(model, scope.enter(reference))
        return reference.copy(model = bundled).inline()
    }
}

/** A copy of this reference pointing at [model], keeping its inline state. */
internal fun Reference.withModel(model: Any?): Reference = copy(model = model).also { it.inline = inline }
