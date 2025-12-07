package dev.banking.asyncapi.generator.core.model.operations

import dev.banking.asyncapi.generator.core.model.references.Reference

data class OperationReply(
    val address: OperationReplyAddressInterface? = null,
    val channel: Reference? = null,
    val messages: List<Reference>? = null,
)
