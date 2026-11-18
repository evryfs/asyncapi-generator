package dev.banking.asyncapi.generator.core.model.operations

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Operation Object written inline or as a `$ref`. */
sealed interface OperationInterface : ReferenceOr<Operation> {
    /**
     * A Operation Object written inline.
     *
     * @property operation the object
     */
    data class OperationInline(
        @get:JsonUnwrapped
        val operation: Operation,
    ) : OperationInterface {
        override fun resolvedOrNull(): Operation = operation

        override fun held(): Any = operation
    }

    /**
     * A `$ref` to a Operation Object.
     *
     * @property reference the reference
     */
    data class OperationReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : OperationInterface {
        override fun resolvedOrNull(): Operation? = reference.finalModel() as? Operation

        override fun held(): Any = reference
    }
}
