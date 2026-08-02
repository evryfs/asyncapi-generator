package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_ACTION_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_ACTION_VALUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_TARGET
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_TRAIT_EMPTY
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.security.SecuritySchemeValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class OperationValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagValidator = TagValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val securitySchemeValidator = SecuritySchemeValidator(asyncApiContext)
    private val operationReplyValidator = OperationReplyValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: OperationInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is OperationInterface.OperationInline ->
                validate(node.operation, contextString, results)

            is OperationInterface.OperationReference ->
                referenceResolver.resolve(node.reference, contextString, results)
        }
    }

    private fun validate(node: Operation, contextString: String, results: ValidationCollector) {
        validateAction(node, contextString, results)
        validateChannel(node, contextString, results)
        validateMessages(node, contextString, results)
        validateReply(node, contextString, results)
        validateTraits(node, contextString, results)
        validateBindings(node, contextString, results)
        validateSecurity(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)
    }

    private fun validateAction(node: Operation, contextString: String, results: ValidationCollector) {
        val action = node.action.let(::sanitizeString)
        if (action.isBlank()) {
            results.error(
                OPERATION_ACTION_REQUIRED,
                "$contextString must define an 'action' field ('send' or 'receive').",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::action),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
        } else if (action != "send" && action != "receive") {
            results.error(
                OPERATION_ACTION_VALUE,
                "$contextString has invalid action '$action'. Allowed values are 'send' or 'receive'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::action),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
        }
    }

    private fun validateChannel(node: Operation, contextString: String, results: ValidationCollector) {
        val channelRef = node.channel
        if (channelRef == null) {
            results.error(
                OPERATION_CHANNEL_REQUIRED,
                "$contextString must define a 'channel' reference.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::channel),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
            return
        }
        val channelRefSourceLocation = asyncApiContext.getSourceLocation(channelRef, channelRef::ref)
        referenceResolver.resolve(channelRef, "Channel", results)
        if (channelRef.model != null && channelRef.model !is Channel) {
            val invalidObject = channelRef.model?.javaClass?.simpleName
            results.error(
                OPERATION_CHANNEL_TARGET,
                "$contextString channel reference must point to a Channel Object. Found: $invalidObject.",
                sourceLocation = channelRefSourceLocation,
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
        }
    }

    private fun validateMessages(node: Operation, contextString: String, results: ValidationCollector) {
        val messages = node.messages ?: return
        messages.forEachIndexed { index, messageReference ->
            val contextString = "$contextString Message[$index]"
            val referenceString = messageReference.ref.let(::sanitizeString)
            if (referenceString.isBlank()) {
                results.error(
                    OPERATION_MESSAGE_REFERENCE,
                    "$contextString 'messages' property value MUST be a list of Reference Objects.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::messages),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
                )
            } else {
                referenceResolver.resolve(messageReference, contextString, results)
            }
        }
    }

    private fun validateReply(node: Operation, contextString: String, results: ValidationCollector) {
        val reply = node.reply ?: return
        val contextString = "$contextString Reply"
        when (reply) {
            is OperationReplyInterface.OperationReplyInline ->
                operationReplyValidator.validate(reply.operationReply, contextString, results)

            is OperationReplyInterface.OperationReplyReference ->
                referenceResolver.resolve(reply.reference, contextString, results)
        }
    }

    private fun validateTraits(node: Operation, contextString: String, results: ValidationCollector) {
        val traits = node.traits ?: return
        traits.forEachIndexed { index, trait ->
            val contextString = "$contextString Trait[$index]"
            when (trait) {
                is OperationTraitInterface.OperationTraitInline ->
                    validateOperationTrait(trait.operationTrait, contextString, results)

                is OperationTraitInterface.OperationTraitReference ->
                    referenceResolver.resolve(trait.reference, contextString, results)
            }
        }
    }

    private fun validateOperationTrait(node: OperationTrait, contextString: String, results: ValidationCollector) {
        if (node.bindings == null && node.security == null && node.tags == null) {
            results.warn(
                OPERATION_TRAIT_EMPTY,
                "$contextString defines no 'bindings', 'security', or 'tags' — may have no effect.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::bindings),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
        }
    }

    private fun validateBindings(node: Operation, contextString: String, results: ValidationCollector) {
        val bindings = node.bindings ?: return
        bindings.forEach { (bindingName, bindingInterface) ->
            val contextString = "$contextString Binding '$bindingName' "
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, contextString, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, contextString, results)
            }
        }
    }

    private fun validateSecurity(node: Operation, contextString: String, results: ValidationCollector) {
        val security = node.security ?: return
        security.forEachIndexed { index, securitySchemeInterface ->
            val contextString = "$contextString Security Scheme [index=$index]"
            when (securitySchemeInterface) {
                is SecuritySchemeInterface.SecuritySchemeInline ->
                    securitySchemeValidator.validate(securitySchemeInterface.security, contextString, results)

                is SecuritySchemeInterface.SecuritySchemeReference ->
                    referenceResolver.resolve(securitySchemeInterface.reference, contextString, results)
            }
        }
    }

    private fun validateTags(node: Operation, contextString: String, results: ValidationCollector) {
        val tags = node.tags ?: return
        tags.forEachIndexed { index, tagInterface ->
            val contextString = "$contextString Tag[$index]"
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, contextString, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(tagInterface.reference, contextString, results)
            }
        }
    }

    private fun validateExternalDocs(node: Operation, contextString: String, results: ValidationCollector) {
        val contextString = "$contextString ExternalDocs"
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(docs.reference, contextString, results)

            null -> {}
        }
    }
}
