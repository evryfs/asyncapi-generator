package dev.banking.asyncapi.generator.core.validator.channels

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.parameters.PARAMETER_PLACEHOLDER
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.PARAMETER
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_ADDRESS_SUFFIX
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_PARAMETER_UNDEFINED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_PARAMETER_UNUSED
import dev.banking.asyncapi.generator.core.validator.parameters.ParameterRules
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Channel Object: its address and parameters, and walks its messages, servers, tags,
 * external documentation, parameters and bindings.
 *
 * @param asyncApiContext the load the channels belong to
 */
internal class ChannelRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<Channel> {
    override fun check(
        node: Channel,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        validateAddress(node, path, walk)
        node.messages?.forEach { (messageName, messageInterface) ->
            walk.member(messageInterface, MESSAGE, path.named("Message", messageName))
        }
        node.servers?.forEachIndexed { index, reference ->
            walk.reference(reference, SERVER, path.indexed("Server", index))
        }
        walk.tags(node.tags, path)
        walk.externalDocs(node.externalDocs, path)
        node.parameters?.forEach { (parameterName, parameterInterface) ->
            val parameterPath = path.named("Parameter", parameterName)
            ParameterRules.checkName(parameterName, parameterInterface, parameterPath, walk)
            walk.member(parameterInterface, PARAMETER, parameterPath)
        }
        walk.bindings(node.bindings, path)
    }

    private fun validateAddress(
        node: Channel,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val address = node.address
        val definedParameters = node.parameters?.keys ?: emptySet()
        if (address == null) {
            reportUnusedParameters(node, definedParameters, emptySet(), path, walk)
            return
        }
        if (address.contains("?") || address.contains("#")) {
            walk.results.error(
                CHANNEL_ADDRESS_SUFFIX,
                "$path address must not contain query parameters or fragments. Use bindings for that.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::address),
            )
        }
        val addressParameters =
            PARAMETER_PLACEHOLDER
                .findAll(address)
                .map { it.groupValues[1] }
                .toSet()
        val missingDefinitions = addressParameters - definedParameters
        if (missingDefinitions.isNotEmpty()) {
            walk.results.error(
                CHANNEL_PARAMETER_UNDEFINED,
                "$path address uses parameters $missingDefinitions which are not defined in channel parameters map.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::address),
            )
        }
        reportUnusedParameters(node, definedParameters, addressParameters, path, walk)
    }

    private fun reportUnusedParameters(
        node: Channel,
        definedParameters: Set<String>,
        addressParameters: Set<String>,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        if (node.parameters != null && addressParameters.isEmpty()) {
            walk.results.error(
                CHANNEL_PARAMETER_UNUSED,
                "$path defines 'parameters', but its address contains no channel address expressions.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::parameters),
            )
            return
        }
        val unusedDefinitions = definedParameters - addressParameters
        if (unusedDefinitions.isNotEmpty()) {
            walk.results.error(
                CHANNEL_PARAMETER_UNUSED,
                "$path defines parameters $unusedDefinitions which are not used in its channel address.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::parameters),
            )
        }
    }
}
