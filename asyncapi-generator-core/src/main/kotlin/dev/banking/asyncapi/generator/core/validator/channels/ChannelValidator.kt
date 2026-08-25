package dev.banking.asyncapi.generator.core.validator.channels

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.PARAMETER_PLACEHOLDER
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_ADDRESS_QUERY_OR_FRAGMENT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_PARAMETER_UNDEFINED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_PARAMETER_UNUSED
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.messages.MessageValidator
import dev.banking.asyncapi.generator.core.validator.parameters.ParameterValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

internal class ChannelValidator(
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
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::address),
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
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::address),
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
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::parameters),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
            )
            return
        }
        val unusedDefinitions = definedParameters - addressParameters
        if (unusedDefinitions.isNotEmpty()) {
            results.error(
                CHANNEL_PARAMETER_UNUSED,
                "$contextString defines parameters $unusedDefinitions which are not used in its channel address.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::parameters),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject",
            )
        }
    }

    private fun validateMessages(node: Channel, contextString: String, results: ValidationCollector) {
        val messages = node.messages ?: return
        messages.forEach { (messageName, messageInterface) ->
            messageValidator.validateInterface(
                messageInterface,
                "$contextString Message '$messageName'",
                results,
            )
        }
    }

    private fun validateServers(node: Channel, contextString: String, results: ValidationCollector) {
        node.servers?.forEachIndexed { index, reference ->
            referenceResolver.resolve(reference, SERVER, "$contextString Server[$index]", results)
        }
    }

    private fun validateTags(node: Channel, contextString: String, results: ValidationCollector) {
        tagValidator.validateList(node.tags, contextString, results)
    }

    private fun validateParameters(node: Channel, contextString: String, results: ValidationCollector) {
        val parameters = node.parameters ?: return
        parameters.forEach { (parameterName, parameterInterface) ->
            val context = "$contextString Parameter '$parameterName'"
            parameterValidator.validateInterface(parameterInterface, context, results, parameterName)
        }
    }

    private fun validateExternalDocs(node: Channel, contextString: String, results: ValidationCollector) {
        val externalDocs = node.externalDocs ?: return
        externalDocsValidator.validateInterface(
            externalDocs,
            "$contextString ExternalDocs",
            results,
        )
    }

    private fun validateBindings(node: Channel, contextString: String, results: ValidationCollector) {
        bindingValidator.validateMap(node.bindings, contextString, results)
    }
}
