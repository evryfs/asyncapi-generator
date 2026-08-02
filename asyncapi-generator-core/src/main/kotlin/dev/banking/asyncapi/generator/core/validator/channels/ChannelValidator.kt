package dev.banking.asyncapi.generator.core.validator.channels

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.PARAMETER_PLACEHOLDER
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_ADDRESS_QUERY_OR_FRAGMENT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_BINDINGS_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_MESSAGES_AMBIGUOUS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_PARAMETER_UNDEFINED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_PARAMETER_UNUSED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_SERVERS_EMPTY
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.messages.MessageValidator
import dev.banking.asyncapi.generator.core.validator.parameters.ParameterValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

class ChannelValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagValidator = TagValidator(asyncApiContext)
    private val messageValidator = MessageValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val parameterValidator = ParameterValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: ChannelInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is ChannelInterface.ChannelInline ->
                validate(node.channel, contextString, results)

            is ChannelInterface.ChannelReference ->
                referenceResolver.resolve(node.reference, CHANNEL, contextString, results)
        }
    }

    private fun validate(node: Channel, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validateAddress(node, contextString, results)
        validateMessages(node, contextString, results)
        validateServers(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)
        validateParameters(node, contextString, results)
        validateBindings(node, contextString, results)
    }

    private fun validateAddress(node: Channel, contextString: String, results: ValidationCollector) {
        val address = node.address
        val definedParameters = node.parameters?.keys ?: emptySet()
        if (address == null) {
            reportUnusedParameters(node, definedParameters, emptySet(), contextString, results)
            return
        }
        if (address.contains("?") || address.contains("#")) {
            results.error(
                CHANNEL_ADDRESS_QUERY_OR_FRAGMENT,
                "$contextString address must not contain query parameters or fragments. Use bindings for that.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::address),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
            )
        }
        val addressParameters = PARAMETER_PLACEHOLDER
            .findAll(address)
            .map { it.groupValues[1] }
            .toSet()
        val missingDefinitions = addressParameters - definedParameters
        if (missingDefinitions.isNotEmpty()) {
            results.error(
                CHANNEL_PARAMETER_UNDEFINED,
                "$contextString address uses parameters $missingDefinitions which are not defined in channel parameters map.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::address),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#parametersObject",
            )
        }
        reportUnusedParameters(node, definedParameters, addressParameters, contextString, results)
    }

    private fun reportUnusedParameters(
        node: Channel,
        definedParameters: Set<String>,
        addressParameters: Set<String>,
        contextString: String,
        results: ValidationCollector,
    ) {
        if (node.parameters != null && addressParameters.isEmpty()) {
            results.error(
                CHANNEL_PARAMETER_UNUSED,
                "$contextString defines 'parameters', but its address contains no channel address expressions.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::parameters),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
            )
            return
        }
        val unusedDefinitions = definedParameters - addressParameters
        if (unusedDefinitions.isNotEmpty()) {
            results.error(
                CHANNEL_PARAMETER_UNUSED,
                "$contextString defines parameters $unusedDefinitions which are not used in its channel address.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::parameters),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
            )
        }
    }

    private fun validateMessages(node: Channel, contextString: String, results: ValidationCollector) {
        val messages = node.messages ?: return
        checkAmbiguity(node, messages, contextString, results)
        messages.forEach { (messageName, messageInterface) ->
            val contextString = "$contextString Message '$messageName'"
            when (messageInterface) {
                is MessageInterface.MessageInline ->
                    messageValidator.validate(messageInterface.message, contextString, results)

                is MessageInterface.MessageReference ->
                    referenceResolver.resolve(messageInterface.reference, MESSAGE, contextString, results)
            }
        }
    }

    private fun validateServers(node: Channel, contextString: String, results: ValidationCollector) {
        val servers = node.servers ?: return
        if (servers.isEmpty()) {
            results.warn(
                CHANNEL_SERVERS_EMPTY,
                "$contextString defines an empty 'servers' array. It will be available on all servers.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::servers),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
            )
        }
        servers.forEachIndexed { index, reference ->
            referenceResolver.resolve(reference, SERVER, "$contextString Server[$index]", results)
        }
    }

    private fun validateTags(node: Channel, contextString: String, results: ValidationCollector) {
        val tags = node.tags ?: return
        tags.forEachIndexed { index, tagInterface ->
            val contextString = "$contextString Tag[$index]"
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, contextString, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(tagInterface.reference, TAG, contextString, results)
            }
        }
    }

    private fun validateParameters(node: Channel, contextString: String, results: ValidationCollector) {
        val parameters = node.parameters ?: return
        parameters.forEach { (parameterName, parameterInterface) ->
            val contextString = "$contextString Parameter '$parameterName'"
            parameterValidator.validateInterface(parameterInterface, contextString, results, parameterName)
        }
    }

    private fun validateExternalDocs(node: Channel, contextString: String, results: ValidationCollector) {
        val contextString = "$contextString ExternalDocs"
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(docs.reference, EXTERNAL_DOC, contextString, results)

            null -> {}
        }
    }

    private fun validateBindings(node: Channel, contextString: String, results: ValidationCollector) {
        val bindings = node.bindings ?: return
        if (bindings.isEmpty()) {
            results.warn(
                CHANNEL_BINDINGS_EMPTY,
                "$contextString defines an empty 'bindings' object. Can be omitted if no bindings are defined.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::bindings),
            )
            return
        }
        bindings.forEach { (bindingName, bindingInterface) ->
            val contextString = "$contextString Binding '$bindingName'"
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, contextString, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, BINDING, contextString, results)
            }
        }
    }

    private fun checkAmbiguity(
        node: Channel,
        messages: Map<String, MessageInterface>,
        contextString: String,
        results: ValidationCollector,
    ) {
        val refMap = mutableMapOf<String, String>()
        messages.forEach { (msgName, msgInterface) ->
            if (msgInterface is MessageInterface.MessageReference) {
                val ref = msgInterface.reference.ref
                if (refMap.containsKey(ref)) {
                    results.warn(
                        CHANNEL_MESSAGES_AMBIGUOUS,
                        "$contextString contains ambiguous messages which may be indistinguishable at runtime.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, node::messages),
                        doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
                    )
                } else {
                    refMap[ref] = msgName
                }
            }
        }
    }
}
