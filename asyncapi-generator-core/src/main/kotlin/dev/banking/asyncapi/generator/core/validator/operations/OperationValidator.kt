package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_TRAIT
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SECURITY_SCHEME
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_ACTION_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_ACTION_VALUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_REFERENCE_SCOPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_TARGET
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_ADDRESS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.security.SecuritySchemeValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

internal class OperationValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagValidator = TagValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val securitySchemeValidator = SecuritySchemeValidator(asyncApiContext)
    private val operationReplyValidator = OperationReplyValidator(asyncApiContext)
    private val operationTraitValidator = OperationTraitValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(
        node: OperationInterface,
        contextString: String,
        results: ValidationCollector,
        rootChannels: Map<String, ChannelInterface>? = null,
    ) {
        when (node) {
            is OperationInterface.OperationInline ->
                validate(node.operation, contextString, results, rootChannels)

            is OperationInterface.OperationReference ->
                referenceResolver.resolve(node.reference, OPERATION, contextString, results)
        }
    }

    private fun validate(
        node: Operation,
        contextString: String,
        results: ValidationCollector,
        rootChannels: Map<String, ChannelInterface>?,
    ) {
        if (!results.visit(node)) return
        validateAction(node, contextString, results)
        val channel = validateChannel(node, contextString, results, rootChannels)
        validateMessages(node, contextString, results, channel)
        validateReply(node, contextString, results, rootChannels)
        validateTraits(node, contextString, results)
        validateBindings(node, contextString, results)
        validateSecurity(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)
    }

    private fun validateAction(node: Operation, contextString: String, results: ValidationCollector) {
        val action = node.action
        if (action.isEmpty()) {
            results.error(
                OPERATION_ACTION_REQUIRED,
                "$contextString must define an 'action' field ('send' or 'receive').",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::action),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
        } else if (action != "send" && action != "receive") {
            results.error(
                OPERATION_ACTION_VALUE,
                "$contextString has invalid action '$action'. Allowed values are 'send' or 'receive'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::action),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
        }
    }

    private fun validateChannel(
        node: Operation,
        contextString: String,
        results: ValidationCollector,
        rootChannels: Map<String, ChannelInterface>?,
    ): Channel? {
        val channelRef = node.channel ?: return null
        val channel = referenceResolver.resolve(
            channelRef,
            CHANNEL,
            "$contextString Channel",
            results,
            targetCategoryRule = OPERATION_CHANNEL_TARGET,
        ) as? Channel
        if (
            channel != null &&
            rootChannels != null &&
            !OperationReferenceBoundary.containsChannel(rootChannels, asyncApiContext.modelTracking.findReference(channelRef))
        ) {
            results.error(
                OPERATION_CHANNEL_REFERENCE_SCOPE,
                "$contextString must reference a channel from the root 'channels' object.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(channelRef, channelRef::ref),
            )
            return null
        }
        return channel
    }

    private fun validateMessages(
        node: Operation,
        contextString: String,
        results: ValidationCollector,
        channel: Channel?,
    ) {
        val messages = node.messages ?: return
        messages.forEachIndexed { index, messageReference ->
            val messageContext = "$contextString Message[$index]"
            val target = referenceResolver.resolve(messageReference, MESSAGE, messageContext, results)
            if (
                target != null &&
                channel != null &&
                !OperationReferenceBoundary.containsMessage(channel, asyncApiContext.modelTracking.findReference(messageReference))
            ) {
                results.error(
                    OPERATION_MESSAGE_REFERENCE,
                    "$messageContext must reference a message from the operation channel's 'messages' object.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(messageReference, messageReference::ref),
                )
            }
        }
    }

    private fun validateReply(
        node: Operation,
        contextString: String,
        results: ValidationCollector,
        rootChannels: Map<String, ChannelInterface>?,
    ) {
        val reply = node.reply ?: return
        val replyContext = "$contextString Reply"
        when (reply) {
            is OperationReplyInterface.OperationReplyInline ->
                operationReplyValidator.validate(reply.operationReply, replyContext, results, rootChannels)

            is OperationReplyInterface.OperationReplyReference ->
                referenceResolver.resolve(reply.reference, OPERATION_REPLY, replyContext, results)
        }
    }

    private fun validateTraits(node: Operation, contextString: String, results: ValidationCollector) {
        node.traits
            ?.forEachIndexed { index, trait ->
                operationTraitValidator.validateInterface(
                    trait,
                    "$contextString Trait[$index]",
                    results,
                )
            }
    }

    private fun validateBindings(node: Operation, contextString: String, results: ValidationCollector) {
        bindingValidator.validateMap(node.bindings, contextString, results)
    }

    private fun validateSecurity(node: Operation, contextString: String, results: ValidationCollector) {
        securitySchemeValidator.validateList(
            node.security ?: return,
            "$contextString Security Scheme",
            results,
        )
    }

    private fun validateTags(node: Operation, contextString: String, results: ValidationCollector) {
        tagValidator.validateList(node.tags, contextString, results)
    }

    private fun validateExternalDocs(node: Operation, contextString: String, results: ValidationCollector) {
        val externalDocs = node.externalDocs ?: return
        externalDocsValidator.validateInterface(
            externalDocs,
            "$contextString ExternalDocs",
            results,
        )
    }
}
