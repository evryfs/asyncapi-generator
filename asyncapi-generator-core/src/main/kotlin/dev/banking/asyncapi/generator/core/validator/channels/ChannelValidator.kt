package dev.banking.asyncapi.generator.core.validator.channels

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.PARAMETER_PLACEHOLDER
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.messages.MessageValidator
import dev.banking.asyncapi.generator.core.validator.parameters.ParameterValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class ChannelValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagValidator = TagValidator(asyncApiContext)
    private val messageValidator = MessageValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val parameterValidator = ParameterValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: ChannelInterface, contextString: String, results: ValidationResults) {
        when (node) {
            is ChannelInterface.ChannelInline ->
                validate(node.channel, contextString, results)

            is ChannelInterface.ChannelReference ->
                referenceResolver.resolve(node.reference, contextString, results)
        }
    }

    fun validateChannels(channels: Map<String, ChannelInterface>, results: ValidationResults) {
        channels.forEach { (channelName, channelInterface) ->
            when (channelInterface) {
                is ChannelInterface.ChannelInline ->
                    validate(channelInterface.channel, channelName, results)

                is ChannelInterface.ChannelReference ->
                    referenceResolver.resolve(channelInterface.reference, "Channel '$channelName'", results)
            }
        }
    }

    private fun validate(node: Channel, channelName: String, results: ValidationResults) {
        validateAddress(node, channelName, results)
        validateMessages(node, channelName, results)
        validateServers(node, channelName, results)
        validateTags(node, channelName, results)
        validateExternalDocs(node, channelName, results)
        validateParameters(node, channelName, results)
        validateBindings(node, channelName, results)
    }

    private fun validateAddress(node: Channel, channelName: String, results: ValidationResults) {
        val address = node.address?.let(::sanitizeString) ?: return
        if (address.isBlank()) {
            results.warn(
                "$channelName does not define an 'address'. It may be treated as dynamically assigned.",
                asyncApiContext.getLine(node, node::address),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
            )
            return
        }
        if (address.contains("?") || address.contains("#")) {
            results.error(
                "$channelName address must not contain query parameters or fragments. Use bindings for that.",
                asyncApiContext.getLine(node, node::address),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
            )
            return
        }
        val definedParameters = node.parameters?.keys ?: emptySet()
        // Extract parameter names from the address (stripping the curly braces)
        val addressParameters = PARAMETER_PLACEHOLDER
            .findAll(address)
            .map { it.groupValues[1] }
            .toSet()
        val missingDefinitions = addressParameters - definedParameters
        if (missingDefinitions.isNotEmpty()) {
            results.error(
                "$channelName address uses parameters $missingDefinitions which are not defined in channel parameters map.",
                asyncApiContext.getLine(node, node::address),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#parametersObject",
            )
        }
        val unusedDefinitions = definedParameters - addressParameters
        if (unusedDefinitions.isNotEmpty()) {
            results.warn(
                "$channelName defines parameters $unusedDefinitions which are not used in the channel address '$address'.",
                asyncApiContext.getLine(node, node::parameters),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
            )
        }
    }

    private fun validateMessages(node: Channel, channelName: String, results: ValidationResults) {
        val messages = node.messages
        if (messages.isNullOrEmpty()) {
            results.error(
                "$channelName' must define at least one message in 'messages'.",
                asyncApiContext.getLine(node, node::messages),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
            )
            return
        }
        checkAmbiguity(node, messages, channelName, results)
        messages.forEach { (messageName, messageInterface) ->
            val contextString = "$channelName Message '$messageName'"
            when (messageInterface) {
                is MessageInterface.MessageInline ->
                    messageValidator.validate(messageInterface.message, contextString, results)

                is MessageInterface.MessageReference ->
                    referenceResolver.resolve(messageInterface.reference, contextString, results)
            }
        }
    }

    private fun validateServers(node: Channel, channelName: String, results: ValidationResults) {
        val servers = node.servers ?: return
        if (servers.isEmpty()) {
            results.warn(
                "$channelName defines an empty 'servers' array. It will be available on all servers.",
                asyncApiContext.getLine(node, node::servers),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
            )
        }
        servers.forEachIndexed { index, reference ->
            referenceResolver.resolve(reference, "$channelName Server [index=$index]", results)
        }
    }

    private fun validateTags(node: Channel, channelName: String, results: ValidationResults) {
        val tags = node.tags ?: return
        if (tags.isEmpty()) {
            results.warn(
                "$channelName defines an empty 'tags' list. Can be omitted if no tags are defined.",
                asyncApiContext.getLine(node, node::tags),
            )
        }
        tags.forEachIndexed { index, tagInterface ->
            val contextString = "Channel $channelName Tag[$index]"
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, contextString, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(tagInterface.reference, contextString, results)
            }
        }
    }

    private fun validateParameters(node: Channel, channelName: String, results: ValidationResults) {
        val parameters = node.parameters ?: return
        if (parameters.isEmpty()) {
            results.warn(
                "$channelName defines an empty 'parameters' map. Can be omitted if no parameters are defined.",
                asyncApiContext.getLine(node, node::parameters),
            )
            return
        }
        parameters.forEach { (parameterName, parameterInterface) ->
            val contextString = "$channelName Parameter '$parameterName'"
            when (parameterInterface) {
                is ParameterInterface.ParameterInline ->
                    parameterValidator.validate(parameterInterface.parameter, contextString, results)

                is ParameterInterface.ParameterReference ->
                    referenceResolver.resolve(parameterInterface.reference, contextString, results)
            }
        }
    }

    private fun validateExternalDocs(node: Channel, channelName: String, results: ValidationResults) {
        val contextString = "$channelName ExternalDocs"
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(docs.reference, contextString, results)

            null -> {}
        }
    }

    private fun validateBindings(node: Channel, channelName: String, results: ValidationResults) {
        val bindings = node.bindings ?: return
        if (bindings.isEmpty()) {
            results.warn(
                "$channelName defines an empty 'bindings' object. Can be omitted if no bindings are defined.",
                asyncApiContext.getLine(node, node::bindings),
            )
            return
        }
        bindings.forEach { (bindingName, bindingInterface) ->
            val contextString = "$channelName Binding '$bindingName'"
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, contextString, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, contextString, results)
            }
        }
    }

    private fun checkAmbiguity(
        node: Channel,
        messages: Map<String, MessageInterface>,
        channelName: String,
        results: ValidationResults,
    ) {
        val refMap = mutableMapOf<String, String>()
        messages.forEach { (msgName, msgInterface) ->
            if (msgInterface is MessageInterface.MessageReference) {
                val ref = msgInterface.reference.ref
                if (refMap.containsKey(ref)) {
                    results.warn(
                        "$channelName contains ambiguous messages which may be indistinguishable at runtime.",
                        asyncApiContext.getLine(node, node::messages),
                        "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject"
                    )
                } else {
                    refMap[ref] = msgName
                }
            }
        }
    }
}
