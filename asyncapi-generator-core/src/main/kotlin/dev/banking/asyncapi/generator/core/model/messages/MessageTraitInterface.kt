package dev.banking.asyncapi.generator.core.model.messages

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference

sealed interface MessageTraitInterface {

    data class InlineMessageTrait(
        @get:JsonUnwrapped
        val trait: MessageTrait,
    ) : MessageTraitInterface

    data class ReferenceMessageTrait(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : MessageTraitInterface
}
