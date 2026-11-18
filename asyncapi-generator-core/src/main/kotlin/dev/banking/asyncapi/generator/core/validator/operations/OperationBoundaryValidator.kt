package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_REFERENCE_SCOPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_REFERENCE
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

/**
 * Checks that root operations, and their replies, point to channels of the root `channels` object,
 * whether the operation or reply is written inline or reached through a `$ref`.
 *
 * Runs after the document walk, so every reference has been resolved. Message subset rules apply to
 * every operation and reply, so they stay with [OperationValidator] and [OperationReplyValidator].
 */
internal class OperationBoundaryValidator(
    private val asyncApiContext: AsyncApiContext,
) {
    fun validate(
        document: AsyncApiDocument,
        results: ValidationCollector,
    ) {
        val rootChannels = document.channels.orEmpty()
        document.operations.orEmpty().forEach { (name, operationInterface) ->
            val operation = resolve(operationInterface) ?: return@forEach
            val contextString = "Operation '$name'"

            operation.channel?.let { channelRef ->
                if (!inRoot(rootChannels, channelRef)) {
                    results.error(
                        OPERATION_CHANNEL_REFERENCE_SCOPE,
                        "$contextString must reference a channel from the root 'channels' object.",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(channelRef, channelRef::ref),
                    )
                }
            }

            val reply = operation.reply?.let(::resolve) ?: return@forEach
            reply.channel?.let { channelRef ->
                if (!inRoot(rootChannels, channelRef)) {
                    results.error(
                        OPERATION_REPLY_CHANNEL_REFERENCE,
                        "$contextString Reply Channel must reference a channel from the root 'channels' object.",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(channelRef, channelRef::ref),
                    )
                }
            }
        }
    }

    /** References that do not resolve to a channel are reported elsewhere; they are treated as in scope here. */
    private fun inRoot(
        rootChannels: Map<String, ChannelInterface>,
        channelRef: Reference,
    ): Boolean {
        if (finalModel(channelRef) !is Channel) return true
        val target = asyncApiContext.modelTracking.findReference(channelRef) ?: return true
        return OperationReferenceBoundary.containsChannel(rootChannels, target)
    }

    private fun resolve(node: OperationInterface): Operation? =
        when (node) {
            is OperationInterface.OperationInline -> node.operation
            is OperationInterface.OperationReference -> finalModel(node.reference) as? Operation
        }

    private fun resolve(node: OperationReplyInterface): OperationReply? =
        when (node) {
            is OperationReplyInterface.OperationReplyInline -> node.operationReply
            is OperationReplyInterface.OperationReplyReference -> finalModel(node.reference) as? OperationReply
        }

    private fun finalModel(reference: Reference): Any? {
        var current: Any? = reference.model
        while (current is Reference) current = current.model
        return current
    }
}
