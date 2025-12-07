package dev.banking.asyncapi.generator.core.validator.channels

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
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility
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

    fun validateChannels(channels: Map<String, ChannelInterface>, results: ValidationResults) {
        channels.forEach { (channelName, channelInterface) ->
            when (channelInterface) {
                is ChannelInterface.ChannelInline ->
                    validate(channelInterface.channel, channelName, results)

                is ChannelInterface.ChannelReference ->
                    referenceResolver.resolve(channelName, channelInterface.reference, "Channel", results)
            }
        }
    }

    private fun validate(node: Channel, channelName: String, results: ValidationResults) {
        validateAddress(node, channelName, results)
        validateMessages(node, channelName,results)
        validateServers(node, channelName,results)
        validateTags(node, channelName, results)
        validateExternalDocs(node, channelName, results)
        validateParameters(node, channelName, results)
        validateBindings(node, channelName, results)
    }

    private fun validateAddress(node: Channel, channelName: String, results: ValidationResults) {
        val address = node.address?.let(::sanitizeString) ?: return
        if (address.isBlank()) {
            results.warn(
                "Channel '$channelName' does not define an 'address'. It will be treated as dynamically assigned.",
                asyncApiContext.getLine(node, node::address)
            )
            return
        }
        if (address.contains("?") || address.contains("#")) {
            results.error(
                "Channel '$channelName' address must not contain query parameters or fragments. Use bindings for that.",
                asyncApiContext.getLine(node, node::address)
            )
            return
        }
        val definedParameters = node.parameters?.keys ?: emptySet()
        val addressParameters = Regex("""\{([^}]+)}""").findAll(address)
            .map { it.groupValues[1] } // groupValues[0] is "{paramName}", groupValues[1] is "paramName"
            .toSet()

        val missingDefinitions = addressParameters - definedParameters
        if (missingDefinitions.isNotEmpty()) {
            results.error(
                "Channel '$channelName' address uses parameters $missingDefinitions which are not defined in 'parameters' map.",
                asyncApiContext.getLine(node, node::address)
            )
        }

        val unusedDefinitions = definedParameters - addressParameters
        if (unusedDefinitions.isNotEmpty()) {
            results.warn(
                "Channel '$channelName' defines parameters $unusedDefinitions which are not used in the address '$address'.",
                asyncApiContext.getLine(node, node::parameters) // Line pointing to the parameters map itself
            )
        }
    }

    private fun validateMessages(node: Channel, channelName: String, results: ValidationResults) {
        val messages = node.messages
        if (messages.isNullOrEmpty()) {
            results.error(
                "Channel '$channelName' must define at least one message in 'messages'.",
                asyncApiContext.getLine(node, node::messages)
            )
            return
        }

        messages.forEach { (_, messageInterface) ->
            when (messageInterface) {
                is MessageInterface.MessageInline ->
                    messageValidator.validate(messageInterface.message, channelName, results)

                is MessageInterface.MessageReference ->
                    referenceResolver.resolve(channelName, messageInterface.reference, "Channel Message", results)
            }
        }
    }

    private fun validateServers(node: Channel, channelName: String, results: ValidationResults) {
        val servers = node.servers ?: return
        if (servers.isEmpty()) {
            results.warn(
                "Channel '$channelName' defines an empty 'servers' array. It will be available on all servers.",
                asyncApiContext.getLine(node, node::servers)
            )
        }

        servers.forEachIndexed { index, reference ->
            referenceResolver.resolve(channelName, reference, "Channel Server [index=$index]", results)
        }
    }

    private fun validateTags(node: Channel, channelName: String, results: ValidationResults) {
        val tags = node.tags ?: return
        if (tags.isEmpty()) {
            results.warn(
                "Channel '$channelName' defines an empty 'tags' list.",
                asyncApiContext.getLine(node, node::tags)
            )
        }

        tags.forEachIndexed { index, tagInterface ->
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, channelName, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(channelName, tagInterface.reference, "Channel Tag [index=$index]", results)
            }
        }
    }

    private fun validateParameters(node: Channel, channelName: String, results: ValidationResults) {
        val parameters = node.parameters ?: return
        if (parameters.isEmpty()) {
            results.warn(
                "Channel '$channelName' defines an empty 'parameters' map.",
                asyncApiContext.getLine(node, node::parameters)
            )
            return
        }

        parameters.forEach { (parameterName, parameterInterface) ->
            when (parameterInterface) {
                is ParameterInterface.ParameterInline ->
                    parameterValidator.validate(parameterInterface.parameter, parameterName, results)

                is ParameterInterface.ParameterReference ->
                    referenceResolver.resolve(channelName, parameterInterface.reference, "Channel Parameter", results)
            }
        }
    }

    private fun validateExternalDocs(node: Channel, channelName: String, results: ValidationResults) {
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, channelName, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(channelName, docs.reference, "Channel ExternalDocs", results)

            null -> {}
        }
    }

    private fun validateBindings(node: Channel, channelName: String, results: ValidationResults) {
        val bindings = node.bindings ?: return
        if (bindings.isEmpty()) {
            results.warn(
                "Channel '$channelName' Message defines an empty 'bindings' object.",
                asyncApiContext.getLine(node, node::bindings),
            )
            return
        }
        bindings.forEach { (bindingName, bindingInterface) ->
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingName, bindingInterface.binding, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(channelName, bindingInterface.reference, "Channel Binding", results)
            }
        }
    }
}
