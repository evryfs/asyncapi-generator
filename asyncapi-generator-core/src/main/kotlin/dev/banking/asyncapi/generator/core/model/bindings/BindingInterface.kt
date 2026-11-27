package dev.banking.asyncapi.generator.core.model.bindings

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Bindings Object written inline or as a `$ref`. */
internal sealed interface BindingInterface : ReferenceOr<Binding> {
    /**
     * A Bindings Object written inline.
     *
     * @property binding the object
     */
    data class BindingInline(
        @get:JsonUnwrapped
        val binding: Binding,
    ) : BindingInterface {
        override fun resolvedOrNull(): Binding = binding

        override fun held(): Any = binding
    }

    /**
     * A `$ref` to a Bindings Object.
     *
     * @property reference the reference
     */
    data class BindingReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : BindingInterface {
        override fun resolvedOrNull(): Binding? = reference.finalModel() as? Binding

        override fun held(): Any = reference
    }
}
