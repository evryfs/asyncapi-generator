package dev.banking.asyncapi.generator.core.parser.linking

import dev.banking.asyncapi.generator.core.model.references.Reference

/**
 * Where following a reference led.
 *
 * Every link has the [chain] of references followed, starting with the reference itself; each one
 * points at the next.
 */
internal sealed interface ReferenceLink {
    val chain: List<Reference>

    /**
     * The chain reached a model of the kind the reference expects.
     *
     * @property chain the references followed
     * @property target the model at the end of the chain
     */
    data class Resolved(
        override val chain: List<Reference>,
        val target: Any,
    ) : ReferenceLink

    /**
     * The last reference of the chain points at no model.
     *
     * @property chain the references followed
     */
    data class Unresolved(
        override val chain: List<Reference>,
    ) : ReferenceLink

    /**
     * The chain reached a model of another kind than the reference expects.
     *
     * @property chain the references followed
     * @property target the model at the end of the chain
     */
    data class WrongTarget(
        override val chain: List<Reference>,
        val target: Any,
    ) : ReferenceLink

    /**
     * The chain reached [target], a reference created for another kind of object.
     *
     * @property chain the references followed
     * @property target the reference of the other kind
     */
    data class CategoryMismatch(
        override val chain: List<Reference>,
        val target: Reference,
    ) : ReferenceLink

    /**
     * The chain reached [target] again, so it never reaches a model.
     *
     * @property chain the references followed
     * @property target the reference that closes the cycle
     */
    data class Cycle(
        override val chain: List<Reference>,
        val target: Reference,
    ) : ReferenceLink
}
