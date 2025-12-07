package com.tietoevry.banking.asyncapi.generator.core.model.messages

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference

sealed interface MessageInterface {

    data class MessageInline(
        @get:JsonUnwrapped
        val message: Message,
    ) : MessageInterface

    data class MessageReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : MessageInterface
}
