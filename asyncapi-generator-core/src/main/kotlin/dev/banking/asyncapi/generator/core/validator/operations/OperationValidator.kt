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
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.security.SecuritySchemeValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
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

    fun validateInterface(node: OperationInterface, contextString: String, results: ValidationResults) {
        when (node) {
            is OperationInterface.OperationInline ->
                validate(node.operation, contextString, results)

            is OperationInterface.OperationReference ->
                referenceResolver.resolve(node.reference, contextString, results)
        }
    }

    fun validateOperations(operations: Map<String, OperationInterface>, results: ValidationResults) {
        operations.forEach { (operationName, operationInterface) ->
            when (operationInterface) {
                is OperationInterface.OperationInline ->
                    validate(operationInterface.operation, operationName, results)

                is OperationInterface.OperationReference ->
                    referenceResolver.resolve(operationInterface.reference, "Operation", results)
            }
        }
    }

    private fun validate(node: Operation, contextString: String, results: ValidationResults) {
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

    private fun validateAction(node: Operation, contextString: String, results: ValidationResults) {
        val action = node.action.let(::sanitizeString)
        if (action.isBlank()) {
            results.error(
                "$contextString must define an 'action' field ('send' or 'receive').",
                asyncApiContext.getLine(node, node::action),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
        } else if (action != "send" && action != "receive") {
            results.error(
                "$contextString has invalid action '$action'. Allowed values are 'send' or 'receive'.",
                asyncApiContext.getLine(node, node::action),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
        }
    }

    private fun validateChannel(node: Operation, contextString: String, results: ValidationResults) {
        val channelRef = node.channel
        if (channelRef == null) {
            results.error(
                "$contextString must define a 'channel' reference.",
                asyncApiContext.getLine(node, node::channel),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
            return
        }
        referenceResolver.resolve(channelRef, "Channel", results)
        if (channelRef.model != null && channelRef.model !is Channel) {
            val invalidObject = channelRef.model?.javaClass?.simpleName
            results.error(
                "$contextString channel reference must point to a Channel Object. Found: $invalidObject.",
                asyncApiContext.getLine(channelRef, channelRef::ref),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
        }
    }

    private fun validateMessages(node: Operation, contextString: String, results: ValidationResults) {
        val messages = node.messages ?: return
        messages.forEachIndexed { index, messageReference ->
            val contextString = "$contextString Message[$index]"
            val referenceString = messageReference.ref.let(::sanitizeString)
            if (referenceString.isBlank()) {
                results.error(
                    "$contextString 'messages' property value MUST be a list of Reference Objects.",
                    asyncApiContext.getLine(node, node::messages),
                    "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
                )
            } else {
                referenceResolver.resolve(messageReference, contextString, results)
            }
        }
    }

    private fun validateReply(node: Operation, contextString: String, results: ValidationResults) {
        val reply = node.reply ?: return
        val contextString = "$contextString Reply"
        when (reply) {
            is OperationReplyInterface.OperationReplyInline ->
                operationReplyValidator.validate(reply.operationReply, contextString, results)

            is OperationReplyInterface.OperationReplyReference ->
                referenceResolver.resolve(reply.reference, contextString, results)
        }
    }

    private fun validateTraits(node: Operation, contextString: String, results: ValidationResults) {
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

    private fun validateOperationTrait(node: OperationTrait, contextString: String, results: ValidationResults) {
        if (node.bindings == null && node.security == null && node.tags == null) {
            results.warn(
                "$contextString defines no 'bindings', 'security', or 'tags' — may have no effect.",
                asyncApiContext.getLine(node, node::bindings),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationObject",
            )
        }
    }

    private fun validateBindings(node: Operation, contextString: String, results: ValidationResults) {
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

    private fun validateSecurity(node: Operation, contextString: String, results: ValidationResults) {
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

    private fun validateTags(node: Operation, contextString: String, results: ValidationResults) {
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

    private fun validateExternalDocs(node: Operation, contextString: String, results: ValidationResults) {
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
