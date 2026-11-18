package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY_ADDRESS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REPLY_CHANNEL_ADDRESS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REPLY_CHANNEL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REPLY_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Operation Reply Object and walks its address, channel and messages.
 *
 * @param asyncApiContext the load the replies belong to
 */
internal class OperationReplyRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<OperationReply> {
    override fun check(
        node: OperationReply,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        node.address?.let { walk.member(it, OPERATION_REPLY_ADDRESS, path.child("Operation Reply Address")) }
        val channel = validateChannel(node, path, walk)
        validateChannelAddress(node, channel, path, walk)
        validateMessages(node, channel, path, walk)
    }

    private fun validateChannel(
        node: OperationReply,
        path: ValidationPath,
        walk: ValidationWalk,
    ): Channel? {
        val channelRef = node.channel
        if (channelRef == null) {
            if (!node.messages.isNullOrEmpty()) {
                walk.results.error(
                    REPLY_CHANNEL_REQUIRED,
                    "$path must define a 'channel' when it defines reply messages.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::messages),
                )
            }
            return null
        }
        return walk.reference(channelRef, CHANNEL, path.child("Channel")) as? Channel
    }

    private fun validateChannelAddress(
        node: OperationReply,
        channel: Channel?,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        if (node.address != null && channel?.address != null) {
            walk.results.error(
                REPLY_CHANNEL_ADDRESS,
                "$path channel must have an unknown address when the reply defines 'address'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::channel),
            )
        }
    }

    private fun validateMessages(
        node: OperationReply,
        channel: Channel?,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val messages = node.messages ?: return
        messages.forEachIndexed { index, messageReference ->
            val messagePath = path.indexed("Message", index)
            val target = walk.reference(messageReference, MESSAGE, messagePath)
            if (
                target != null &&
                channel != null &&
                !OperationReferenceBoundary.containsMessage(
                    channel,
                    asyncApiContext.modelTracking.findReference(messageReference),
                )
            ) {
                walk.results.error(
                    REPLY_MESSAGE_REFERENCE,
                    "$messagePath must reference a message from the reply channel's 'messages' object.",
                    sourceLocation =
                        asyncApiContext.modelTracking.getSourceLocation(
                            messageReference,
                            messageReference::ref,
                        ),
                )
            }
        }
    }
}
