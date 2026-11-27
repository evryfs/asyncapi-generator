package dev.banking.asyncapi.generator.core.model.channels

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Channel Object written inline or as a `$ref`. */
internal sealed interface ChannelInterface : ReferenceOr<Channel> {
    /**
     * A Channel Object written inline.
     *
     * @property channel the object
     */
    data class ChannelInline(
        @get:JsonUnwrapped
        val channel: Channel,
    ) : ChannelInterface {
        override fun resolvedOrNull(): Channel = channel

        override fun held(): Any = channel
    }

    /**
     * A `$ref` to a Channel Object.
     *
     * @property reference the reference
     */
    data class ChannelReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : ChannelInterface {
        override fun resolvedOrNull(): Channel? = reference.finalModel() as? Channel

        override fun held(): Any = reference
    }
}
