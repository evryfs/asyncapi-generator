package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY_ADDRESS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_MESSAGES_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_ADDRESS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

internal class OperationReplyValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val operationReplyAddressValidator = OperationReplyAddressValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: OperationReplyInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is OperationReplyInterface.OperationReplyInline ->
                validate(node.operationReply, contextString, results)

            is OperationReplyInterface.OperationReplyReference ->
                referenceResolver.resolve(node.reference, OPERATION_REPLY, contextString, results)
        }
    }

    fun validate(
        node: OperationReply,
        contextString: String,
        results: ValidationCollector,
        rootChannels: Map<String, ChannelInterface>? = null,
    ) {
        if (!results.visit(node)) return
        validateAddress(node, contextString, results)
        val channel = validateChannel(node, contextString, results, rootChannels)
        validateChannelAddress(node, channel, contextString, results)
        validateMessages(node, channel, contextString, results)
    }

    private fun validateAddress(node: OperationReply, contextString: String, results: ValidationCollector) {
        val address = node.address ?: return
        val contextString = "$contextString Operation Reply Address"
        when (address) {
            is OperationReplyAddressInterface.OperationReplyAddressInline ->
                operationReplyAddressValidator.validate(address.operationReplyAddress, contextString, results)

            is OperationReplyAddressInterface.OperationReplyAddressReference ->
                referenceResolver.resolve(
                    address.reference,
                    OPERATION_REPLY_ADDRESS,
                    contextString,
                    results,
                )
        }
    }

    private fun validateChannel(
        node: OperationReply,
        contextString: String,
        results: ValidationCollector,
        rootChannels: Map<String, ChannelInterface>?,
    ): Channel? {
        val channelRef = node.channel
        if (channelRef == null) {
            if (!node.messages.isNullOrEmpty()) {
                results.error(
                    OPERATION_REPLY_CHANNEL_REQUIRED,
                    "$contextString must define a 'channel' when it defines reply messages.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::messages),
                )
            }
            return null
        }
        val contextString = "$contextString Channel"
        val channel = referenceResolver.resolve(channelRef, CHANNEL, contextString, results) as? Channel
        if (
            channel != null &&
            rootChannels != null &&
            !OperationReferenceBoundary.containsChannel(rootChannels, asyncApiContext.findReference(channelRef))
        ) {
            results.error(
                OPERATION_REPLY_CHANNEL_REFERENCE,
                "$contextString must reference a channel from the root 'channels' object.",
                sourceLocation = asyncApiContext.getSourceLocation(channelRef, channelRef::ref),
            )
            return null
        }
        return channel
    }

    private fun validateChannelAddress(
        node: OperationReply,
        channel: Channel?,
        contextString: String,
        results: ValidationCollector,
    ) {
        if (node.address != null && channel?.address != null) {
            results.error(
                OPERATION_REPLY_CHANNEL_ADDRESS,
                "$contextString channel must have an unknown address when the reply defines 'address'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::channel),
            )
        }
    }

    private fun validateMessages(
        node: OperationReply,
        channel: Channel?,
        operationReplyName: String,
        results: ValidationCollector,
    ) {
        val messages = node.messages ?: return
        if (messages.isEmpty()) {
            results.warn(
                OPERATION_REPLY_MESSAGES_EMPTY,
                "$operationReplyName 'messages' is an empty list — omit it if unused.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::messages),
            )
            return
        }
        messages.forEachIndexed { index, messageReference ->
            val contextString = "$operationReplyName Message[$index]"
            val target = referenceResolver.resolve(messageReference, MESSAGE, contextString, results)
            if (
                target != null &&
                channel != null &&
                !OperationReferenceBoundary.containsMessage(channel, asyncApiContext.findReference(messageReference))
            ) {
                results.error(
                    OPERATION_REPLY_MESSAGE_REFERENCE,
                    "$contextString must reference a message from the reply channel's 'messages' object.",
                    sourceLocation = asyncApiContext.getSourceLocation(messageReference, messageReference::ref),
                )
            }
        }
    }
}
