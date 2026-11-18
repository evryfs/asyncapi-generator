package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_TRAIT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_ACTION_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_ACTION_VALUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_TARGET
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Operation Object and walks its channel, messages, reply, traits, bindings, security,
 * tags and external documentation.
 *
 * @param asyncApiContext the load the operations belong to
 */
internal class OperationRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<Operation> {
    override fun check(
        node: Operation,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        validateAction(node, path, walk)
        val channel = validateChannel(node, path, walk)
        validateMessages(node, path, walk, channel)
        node.reply?.let { walk.member(it, OPERATION_REPLY, path.child("Reply")) }
        node.traits?.forEachIndexed { index, trait ->
            walk.member(trait, OPERATION_TRAIT, path.indexed("Trait", index))
        }
        walk.bindings(node.bindings, path)
        walk.securitySchemes(node.security, path)
        walk.tags(node.tags, path)
        walk.externalDocs(node.externalDocs, path)
    }

    private fun validateAction(
        node: Operation,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val action = node.action
        if (action.isBlank()) {
            walk.results.error(
                OPERATION_ACTION_REQUIRED,
                "$path must define an 'action' field ('send' or 'receive').",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::action),
            )
        } else if (action != "send" && action != "receive") {
            walk.results.error(
                OPERATION_ACTION_VALUE,
                "$path has invalid action '$action'. Allowed values are 'send' or 'receive'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::action),
            )
        }
    }

    private fun validateChannel(
        node: Operation,
        path: ValidationPath,
        walk: ValidationWalk,
    ): Channel? {
        val channelRef = node.channel ?: return null
        return walk.reference(channelRef, CHANNEL, path.child("Channel"), OPERATION_CHANNEL_TARGET) as? Channel
    }

    private fun validateMessages(
        node: Operation,
        path: ValidationPath,
        walk: ValidationWalk,
        channel: Channel?,
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
                    OPERATION_MESSAGE_REFERENCE,
                    "$messagePath must reference a message from the operation channel's 'messages' object.",
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
