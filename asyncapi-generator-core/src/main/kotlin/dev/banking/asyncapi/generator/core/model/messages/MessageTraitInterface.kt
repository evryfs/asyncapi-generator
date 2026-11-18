package dev.banking.asyncapi.generator.core.model.messages

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference

sealed interface MessageTraitInterface {
    data class MessageTraitInline(
        @get:JsonUnwrapped
        val messageTrait: MessageTrait,
    ) : MessageTraitInterface

    data class MessageTraitReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : MessageTraitInterface
}
