package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface

/** Direct ownership checks required by root operation and channel-subset reference rules. */
internal object OperationReferenceBoundary {

    fun containsChannel(channels: Map<String, ChannelInterface>, directTarget: Any?): Boolean =
        channels.values.any { channel -> channel.ownedTarget() === directTarget }

    fun containsMessage(channel: Channel, directTarget: Any?): Boolean =
        channel.messages.orEmpty().values.any { message -> message.ownedTarget() === directTarget }

    private fun ChannelInterface.ownedTarget(): Any =
        when (this) {
            is ChannelInterface.ChannelInline -> channel
            is ChannelInterface.ChannelReference -> reference
        }

    private fun MessageInterface.ownedTarget(): Any =
        when (this) {
            is MessageInterface.MessageInline -> message
            is MessageInterface.MessageReference -> reference
        }
}
