package dev.banking.asyncapi.generator.core.model.messages

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Message Object written inline or as a `$ref`. */
internal sealed interface MessageInterface : ReferenceOr<Message> {
    /**
     * A Message Object written inline.
     *
     * @property message the object
     */
    data class MessageInline(
        @get:JsonUnwrapped
        val message: Message,
    ) : MessageInterface {
        override fun resolvedOrNull(): Message = message

        override fun held(): Any = message
    }

    /**
     * A `$ref` to a Message Object.
     *
     * @property reference the reference
     */
    data class MessageReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : MessageInterface {
        override fun resolvedOrNull(): Message? = reference.finalModel() as? Message

        override fun held(): Any = reference
    }
}
