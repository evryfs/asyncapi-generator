package dev.banking.asyncapi.generator.core.model.operations

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Operation Reply Object written inline or as a `$ref`. */
internal sealed interface OperationReplyInterface : ReferenceOr<OperationReply> {
    /**
     * A Operation Reply Object written inline.
     *
     * @property operationReply the object
     */
    data class OperationReplyInline(
        @get:JsonUnwrapped
        val operationReply: OperationReply,
    ) : OperationReplyInterface {
        override fun resolvedOrNull(): OperationReply = operationReply

        override fun held(): Any = operationReply
    }

    /**
     * A `$ref` to a Operation Reply Object.
     *
     * @property reference the reference
     */
    data class OperationReplyReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : OperationReplyInterface {
        override fun resolvedOrNull(): OperationReply? = reference.finalModel() as? OperationReply

        override fun held(): Any = reference
    }
}
