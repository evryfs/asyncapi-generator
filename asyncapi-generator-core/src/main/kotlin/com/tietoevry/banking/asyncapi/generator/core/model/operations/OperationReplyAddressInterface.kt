package com.tietoevry.banking.asyncapi.generator.core.model.operations

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference

sealed interface OperationReplyAddressInterface {

    data class OperationReplyAddressInline(
        @get:JsonUnwrapped
        val operationReplyAddress: OperationReplyAddress,
    ) : OperationReplyAddressInterface

    data class OperationReplyAddressReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : OperationReplyAddressInterface
}
