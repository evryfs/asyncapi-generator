package dev.banking.asyncapi.generator.core.model.operations

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Operation Reply Address Object written inline or as a `$ref`. */
sealed interface OperationReplyAddressInterface : ReferenceOr<OperationReplyAddress> {
    /**
     * A Operation Reply Address Object written inline.
     *
     * @property operationReplyAddress the object
     */
    data class OperationReplyAddressInline(
        @get:JsonUnwrapped
        val operationReplyAddress: OperationReplyAddress,
    ) : OperationReplyAddressInterface {
        override fun resolvedOrNull(): OperationReplyAddress = operationReplyAddress

        override fun held(): Any = operationReplyAddress
    }

    /**
     * A `$ref` to a Operation Reply Address Object.
     *
     * @property reference the reference
     */
    data class OperationReplyAddressReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : OperationReplyAddressInterface {
        override fun resolvedOrNull(): OperationReplyAddress? = reference.finalModel() as? OperationReplyAddress

        override fun held(): Any = reference
    }
}
