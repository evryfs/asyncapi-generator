package dev.banking.asyncapi.generator.core.validator.components

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.REFERENCE
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.channels.ChannelValidator
import dev.banking.asyncapi.generator.core.validator.correlations.CorrelationIdValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.messages.MessageTraitValidator
import dev.banking.asyncapi.generator.core.validator.messages.MessageValidator
import dev.banking.asyncapi.generator.core.validator.operations.OperationReplyAddressValidator
import dev.banking.asyncapi.generator.core.validator.operations.OperationReplyValidator
import dev.banking.asyncapi.generator.core.validator.operations.OperationTraitValidator
import dev.banking.asyncapi.generator.core.validator.operations.OperationValidator
import dev.banking.asyncapi.generator.core.validator.parameters.ParameterValidator
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaValidator
import dev.banking.asyncapi.generator.core.validator.security.SecuritySchemeValidator
import dev.banking.asyncapi.generator.core.validator.servers.ServerValidator
import dev.banking.asyncapi.generator.core.validator.servers.ServerVariableValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach

internal class ComponentValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val schemaValidator = SchemaValidator(asyncApiContext)
    private val messageValidator = MessageValidator(asyncApiContext)
    private val parameterValidator = ParameterValidator(asyncApiContext)
    private val securitySchemeValidator = SecuritySchemeValidator(asyncApiContext)
    private val operationValidator = OperationValidator(asyncApiContext)
    private val serverValidator = ServerValidator(asyncApiContext)
    private val serverVariableValidator = ServerVariableValidator(asyncApiContext)
    private val channelValidator = ChannelValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val correlationIdValidator = CorrelationIdValidator(asyncApiContext)
    private val operationReplyValidator = OperationReplyValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val tagValidator = TagValidator(asyncApiContext)
    private val operationReplyAddressValidator = OperationReplyAddressValidator(asyncApiContext)
    private val operationTraitValidator = OperationTraitValidator(asyncApiContext)
    private val messageTraitValidator = MessageTraitValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: ComponentInterface, contextString: String, results: ValidationCollector) {
        val component: Component? = when (node) {
            is ComponentInterface.ComponentInline ->
                node.component

            is ComponentInterface.ComponentReference -> {
                referenceResolver.resolve(node.reference, REFERENCE, contextString, results)
                return
            }
        }
        if (component == null) return
        if (!results.visit(component)) return

        validateSchemas(component, contextString, results)
        validateServers(component, contextString, results)
        validateChannels(component, contextString, results)
        validateOperations(component, contextString, results)
        validateMessages(component, contextString, results)
        validateSecuritySchemes(component, contextString, results)
        validateServerVariables(component, contextString, results)
        validateParameters(component, contextString, results)
        validateCorrelationIds(component, contextString, results)
        validateReplies(component, contextString, results)
        validateOperationReplyAddresses(component, contextString, results)
        validateExternalDocs(component, contextString, results)
        validateTags(component, contextString, results)
        validateOperationTraits(component, contextString, results)
        validateMessageTraits(component, contextString, results)
        validateBindings(component, contextString, results)
    }

    private fun validateSchemas(component: Component, contextString: String, results: ValidationCollector) {
        component.schemas?.let { schemas ->
            schemaValidator.validateMap(schemas, contextString, results)
        }
    }

    private fun validateMessages(component: Component, contextString: String, results: ValidationCollector) {
        component.messages?.forEach { (messageName, messageInterface) ->
            val messageContext = "$contextString Message '$messageName'"
            messageValidator.validateInterface(messageInterface, messageContext, results)
        }
    }

    private fun validateParameters(component: Component, contextString: String, results: ValidationCollector) {
        component.parameters?.forEach { (parameterName, parameterInterface) ->
            val contextString = "$contextString Parameter '$parameterName'"
            parameterValidator.validateInterface(parameterInterface, contextString, results, parameterName)
        }
    }

    private fun validateSecuritySchemes(component: Component, contextString: String, results: ValidationCollector) {
        component.securitySchemes?.forEach { (securitySchemeName, securitySchemeInterface) ->
            val context = "$contextString Security Scheme '$securitySchemeName'"
            securitySchemeValidator.validateInterface(securitySchemeInterface, context, results)
        }
    }

    private fun validateOperations(component: Component, contextString: String, results: ValidationCollector) {
        component.operations?.forEach { (operationName, operationInterface) ->
            val contextString = "$contextString Operation '$operationName'"
            operationValidator.validateInterface(operationInterface, contextString, results)
        }
    }

    private fun validateServers(component: Component, contextString: String, results: ValidationCollector) {
        component.servers?.forEach { (serverName, serverInterface) ->
            val contextString = "$contextString Server '$serverName'"
            serverValidator.validateInterface(serverInterface, contextString, results, serverName)
        }
    }

    private fun validateServerVariables(component: Component, contextString: String, results: ValidationCollector) {
        component.serverVariables?.forEach { (serverVariableName, serverVariableInterface) ->
            val context = "$contextString Server Variable '$serverVariableName'"
            serverVariableValidator.validateInterface(serverVariableInterface, context, results)
        }
    }

    private fun validateCorrelationIds(component: Component, contextString: String, results: ValidationCollector) {
        component.correlationIds?.forEach { (correlationIdName, correlationIdInterface) ->
            val contextString = "$contextString Correlation ID '$correlationIdName'"
            correlationIdValidator.validateInterface(correlationIdInterface, contextString, results)
        }
    }

    private fun validateReplies(component: Component, contextString: String, results: ValidationCollector) {
        component.replies?.forEach { (operationReplyName, operationReplyInterface) ->
            val contextString = "$contextString Operation Reply '$operationReplyName'"
            operationReplyValidator.validateInterface(operationReplyInterface, contextString, results)
        }
    }

    private fun validateChannels(component: Component, contextString: String, results: ValidationCollector) {
        component.channels?.forEach { (channelName, channelInterface) ->
            val contextString = "$contextString Channel '$channelName'"
            channelValidator.validateInterface(channelInterface, contextString, results)
        }
    }

    private fun validateExternalDocs(component: Component, contextString: String, results: ValidationCollector) {
        component.externalDocs?.forEach { (externalDocName, externalDocInterface) ->
            val contextString = "$contextString External Doc '$externalDocName'"
            externalDocsValidator.validateInterface(externalDocInterface, contextString, results)
        }
    }

    private fun validateTags(component: Component, contextString: String, results: ValidationCollector) {
        component.tags?.forEach { (tagName, tagInterface) ->
            val contextString = "$contextString Tag '$tagName'"
            tagValidator.validateInterface(tagInterface, contextString, results)
        }
    }

    private fun validateOperationReplyAddresses(component: Component, contextString: String, results: ValidationCollector) {
        component.replyAddresses?.forEach { (operationReplyAddressName, operationReplyAddressInterface) ->
            val contextString = "$contextString Operation Reply Address '$operationReplyAddressName'"
            operationReplyAddressValidator.validateInterface(operationReplyAddressInterface, contextString, results)
        }
    }

    private fun validateOperationTraits(component: Component, contextString: String, results: ValidationCollector) {
        component.operationTraits?.forEach { (operationTraitName, operationTraitInterface) ->
            val contextString = "$contextString Operation Trait '$operationTraitName'"
            operationTraitValidator.validateInterface(operationTraitInterface, contextString, results)
        }
    }

    private fun validateMessageTraits(component: Component, contextString: String, results: ValidationCollector) {
        component.messageTraits?.forEach { (messageTraitName, messageTraitInterface) ->
            val contextString = "$contextString Message Trait '$messageTraitName'"
            messageTraitValidator.validateInterface(messageTraitInterface, contextString, results)
        }
    }

    private fun validateBindings(component: Component, contextString: String, results: ValidationCollector) {
        component.serverBindings?.forEach { (bindingName, bindingInterface) ->
            bindingValidator.validateInterface(
                bindingInterface,
                "$contextString Server Binding '$bindingName'",
                results,
            )
        }
        component.channelBindings?.forEach { (bindingName, bindingInterface) ->
            bindingValidator.validateInterface(
                bindingInterface,
                "$contextString Channel Binding '$bindingName'",
                results,
            )
        }
        component.operationBindings?.forEach { (bindingName, bindingInterface) ->
            bindingValidator.validateInterface(
                bindingInterface,
                "$contextString Operation Binding '$bindingName'",
                results,
            )
        }
        component.messageBindings?.forEach { (bindingName, bindingInterface) ->
            bindingValidator.validateInterface(
                bindingInterface,
                "$contextString Message Binding '$bindingName'",
                results,
            )
        }
    }
}
