package dev.banking.asyncapi.generator.core.model.messages

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Message Trait Object written inline or as a `$ref`. */
internal sealed interface MessageTraitInterface : ReferenceOr<MessageTrait> {
    /**
     * A Message Trait Object written inline.
     *
     * @property messageTrait the object
     */
    data class MessageTraitInline(
        @get:JsonUnwrapped
        val messageTrait: MessageTrait,
    ) : MessageTraitInterface {
        override fun resolvedOrNull(): MessageTrait = messageTrait

        override fun held(): Any = messageTrait
    }

    /**
     * A `$ref` to a Message Trait Object.
     *
     * @property reference the reference
     */
    data class MessageTraitReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : MessageTraitInterface {
        override fun resolvedOrNull(): MessageTrait? = reference.finalModel() as? MessageTrait

        override fun held(): Any = reference
    }
}
