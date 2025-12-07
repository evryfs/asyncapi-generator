package dev.banking.asyncapi.generator.core.model.operations

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference

sealed interface OperationReplyInterface {

    data class OperationReplyInline(
        @get:JsonUnwrapped
        val operationReply: OperationReply,
    ) : OperationReplyInterface

    data class OperationReplyReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : OperationReplyInterface
}
