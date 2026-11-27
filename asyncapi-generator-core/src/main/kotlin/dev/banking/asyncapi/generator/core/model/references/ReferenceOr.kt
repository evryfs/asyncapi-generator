package dev.banking.asyncapi.generator.core.model.references

/**
 * A member that holds either an inline [T] or a [Reference] to one.
 */
internal interface ReferenceOr<out T : Any> {
    /** The inline model, or the model at the end of the reference's chain; `null` when that is not a [T]. */
    fun resolvedOrNull(): T?

    /** What the member holds: the inline model or the reference itself. */
    fun held(): Any
}
