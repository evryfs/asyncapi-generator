package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
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
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateOperations(operations: Map<String, OperationInterface>, results: ValidationResults) {
        operations.forEach { (operationName, operationInterface) ->
            when (operationInterface) {
                is OperationInterface.OperationInline ->
                    validate(operationInterface.operation, operationName, results)

                is OperationInterface.OperationReference ->
                    referenceResolver.resolve(operationName, operationInterface.reference, "Operation", results)
            }
        }
    }

    private fun validate(node: Operation, operationName: String, results: ValidationResults) {
        validateAction(node, operationName, results)
        validateChannel(node, operationName, results)
        validateMessages(node, operationName, results)
        validateReply(node, operationName, results)
        validateTraits(node, operationName, results)
        validateBindings(node, operationName, results)
        validateSecurity(node, operationName, results)
        validateTags(node, operationName, results)
        validateExternalDocs(node, operationName, results)
    }

    private fun validateAction(node: Operation, operationName: String, results: ValidationResults) {
        val action = node.action.let(::sanitizeString)
        if (action.isBlank()) {
            results.error(
                "Operation '$operationName' must define an 'action' field ('send' or 'receive').",
                asyncApiContext.getLine(node, node::action)
            )
        } else if (action != "send" && action != "receive") {
            results.error(
                "Operation '$operationName' has invalid action '$action'. Allowed values are 'send' or 'receive'.",
                asyncApiContext.getLine(node, node::action)
            )
        }
    }

    private fun validateChannel(node: Operation, operationName: String, results: ValidationResults) {
        val channelRef = node.channel
        if (channelRef == null) {
            results.error(
                "Operation '$operationName' must define a 'channel' reference.",
                asyncApiContext.getLine(node, node::channel)
            )
            return
        }
        referenceResolver.resolve(operationName, channelRef, "Channel", results)
        if (channelRef.model != null && channelRef.model !is Channel) {
            results.error(
                "Operation '$operationName' channel reference must point to a Channel Object. " +
                    "Found: ${channelRef.model?.javaClass?.simpleName}",
                asyncApiContext.getLine(channelRef, channelRef::ref)
            )
        }
    }

    private fun validateMessages(node: Operation, operationName: String, results: ValidationResults) {
        val messages = node.messages
            ?: return
        if (messages.isEmpty()) {
            results.warn(
                "Operation '$operationName' defines an empty 'messages' list.",
                asyncApiContext.getLine(node, node::messages)
            )
            return
        }
        messages.forEachIndexed { index, msgRef ->
            val refString = msgRef.ref.let(::sanitizeString)
            if (refString.isBlank()) {
                results.error(
                    "Operation '$operationName' 'messages' property value MUST be a list of Reference Objects. " +
                        "Found an inline Message Object or invalid reference at index $index.",
                    asyncApiContext.getLine(node, node::messages)
                )
            } else {
                referenceResolver.resolve(operationName, msgRef, "Operation Message [index=$index]", results)
            }
        }
    }

    private fun validateReply(node: Operation, operationName: String, results: ValidationResults) {
        val reply = node.reply
            ?: return
        when (reply) {
            is OperationReplyInterface.OperationReplyInline ->
                validateReplyInline(reply.operationReply, operationName, results)

            is OperationReplyInterface.OperationReplyReference ->
                referenceResolver.resolve(operationName, reply.reference, "Operation Reply", results)
        }
    }

    private fun validateReplyInline(node: OperationReply, operationName: String, results: ValidationResults) {
        val channelRef = node.channel
        if (channelRef == null) {
            results.error(
                "Operation reply must define a 'channel' reference.",
                asyncApiContext.getLine(node, node::channel)
            )
        } else {
            referenceResolver.resolve(operationName, channelRef, "Operation Reply", results)
        }

        val messages = node.messages
        if (messages == null || messages.isEmpty()) {
            results.warn(
                "Operation reply defines no 'messages' — expected at least one.",
                asyncApiContext.getLine(node, node::messages)
            )
        } else {
            messages.forEachIndexed { index, msgRef ->
                referenceResolver.resolve(operationName, msgRef, "Operation Reply Message [index=$index]", results)
            }
        }
    }

    private fun validateTraits(node: Operation, operationName: String, results: ValidationResults) {
        val traits = node.traits ?: return
        if (traits.isEmpty()) {
            results.warn(
                "Operation '${operationName}' defines an empty 'traits' list.",
                asyncApiContext.getLine(node, node::traits)
            )
            return
        }

        traits.forEachIndexed { index, trait ->
            when (trait) {
                is OperationTraitInterface.OperationTraitInline ->
                    validateOperationTrait(trait.operationTrait, operationName, results)

                is OperationTraitInterface.OperationTraitReference ->
                    referenceResolver.resolve(operationName, trait.reference, "Operation Trait [index=$index]", results)
            }
        }
    }

    private fun validateOperationTrait(node: OperationTrait, operationName: String, results: ValidationResults) {
        if (node.bindings == null && node.security == null && node.tags == null) {
            results.warn(
                "OperationTrait in '$operationName' defines no 'bindings', 'security', or 'tags' — may have no effect.",
                asyncApiContext.getLine(node, node::bindings)
            )
        }
    }

    private fun validateBindings(node: Operation, operationName: String, results: ValidationResults) {
        val bindings = node.bindings ?: return
        if (bindings.isEmpty()) {
            results.warn(
                "Operation '$operationName' defines an empty 'bindings' object.",
                asyncApiContext.getLine(node, node::bindings)
            )
            return
        }
        bindings.forEach { (bindingName, bindingInterface) ->
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingName, bindingInterface.binding, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(operationName, bindingInterface.reference, "Operation Binding", results)
            }
        }
    }

    private fun validateSecurity(node: Operation, operationName: String, results: ValidationResults) {
        val security = node.security ?: return
        if (security.isEmpty()) {
            results.warn(
                "Operation '$operationName' defines an empty 'security' list.",
                asyncApiContext.getLine(node, node::security)
            )
        }
        security.forEachIndexed { index, secInterface ->
            when (secInterface) {
                is SecuritySchemeInterface.SecuritySchemeInline ->
                    securitySchemeValidator.validate(secInterface.security, operationName, results)

                is SecuritySchemeInterface.SecuritySchemeReference ->
                    referenceResolver.resolve(operationName, secInterface.reference, "Security Scheme [index=$index]", results)
            }
        }
    }

    private fun validateTags(node: Operation, operationName: String, results: ValidationResults) {
        val tags = node.tags ?: return
        if (tags.isEmpty()) {
            results.warn(
                "Operation '$operationName' defines an empty 'tags' list.",
                asyncApiContext.getLine(node, node::tags)
            )
            return
        }
        tags.forEachIndexed { index, tagInterface ->
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, operationName, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(operationName, tagInterface.reference, "Operation Tag [index=$index]", results)
            }
        }
    }

    private fun validateExternalDocs(node: Operation, operationName: String, results: ValidationResults) {
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, operationName, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(operationName, docs.reference, "Operation ExternalDocs", results)

            null -> {}
        }
    }
}
