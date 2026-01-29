package dev.banking.asyncapi.generator.core.validator.components

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
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
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach

class ComponentValidator(
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

    fun validate(node: ComponentInterface, results: ValidationResults) {
        val component: Component? = when (node) {
            is ComponentInterface.ComponentInline ->
                node.component

            is ComponentInterface.ComponentReference ->
                return
        }
        if (component == null) return

        validateSchemas(component, results)
        validateServers(component, results)
        validateChannels(component, results)
        validateOperations(component, results)
        validateMessages(component, results)
        validateSecuritySchemes(component, results)
        validateServerVariables(component, results)
        validateParameters(component, results)
        validateCorrelationIds(component, results)
        validateReplies(component, results)
        validateOperationReplyAddresses(component, results)
        validateExternalDocs(component, results)
        validateTags(component, results)
        validateOperationTraits(component, results)
        validateMessageTraits(component, results)
        validateBindings(component, results)
    }

    private fun validateSchemas(component: Component, results: ValidationResults) {
        component.schemas?.forEach { (schemaName, schemaInterface) ->
            val contextString = "Component Schema '$schemaName'"
            when (schemaInterface) {
                is SchemaInterface.SchemaInline ->
                    schemaValidator.validate(schemaInterface.schema, contextString, results)

                is SchemaInterface.MultiFormatSchemaInline -> {}

                is SchemaInterface.SchemaReference ->
                    referenceResolver.resolve(schemaInterface.reference, contextString, results)

                is SchemaInterface.BooleanSchema -> {}
            }
        }
    }

    private fun validateMessages(component: Component, results: ValidationResults) {
        component.messages?.forEach { (messageName, messageInterface) ->
            val contextString = "Component Message '$messageName'"
            when (messageInterface) {
                is MessageInterface.MessageInline ->
                    messageValidator.validate(messageInterface.message, contextString, results)

                is MessageInterface.MessageReference -> {
                    referenceResolver.resolve(messageInterface.reference, contextString, results)
                }
            }
        }
    }

    private fun validateParameters(component: Component, results: ValidationResults) {
        component.parameters?.forEach { (parameterName, parameterInterface) ->
            val contextString = "Component Parameter '$parameterName'"
            parameterValidator.validateInterface(parameterInterface, contextString, results)
        }
    }

    private fun validateSecuritySchemes(component: Component, results: ValidationResults) {
        component.securitySchemes?.forEach { (securitySchemeName, securitySchemeInterface) ->
            val contextString = "Component Security Scheme '$securitySchemeName'"
            when (securitySchemeInterface) {
                is SecuritySchemeInterface.SecuritySchemeInline ->
                    securitySchemeValidator.validate(securitySchemeInterface.security, contextString, results)

                is SecuritySchemeInterface.SecuritySchemeReference -> {
                    referenceResolver.resolve(securitySchemeInterface.reference, contextString, results)
                }
            }
        }
    }

    private fun validateOperations(component: Component, results: ValidationResults) {
        component.operations?.forEach { (operationName, operationInterface) ->
            val contextString = "Component Operation '$operationName'"
            operationValidator.validateInterface(operationInterface, contextString, results)
        }
    }

    private fun validateServers(component: Component, results: ValidationResults) {
        component.servers?.let { serverValidator.validateServers(it, results) }
    }

    private fun validateServerVariables(component: Component, results: ValidationResults) {
        component.serverVariables?.forEach { (serverVariableName, serverVariableInterface) ->
            val contextString = "Component Server Variable '$serverVariableName'"
            when (serverVariableInterface) {
                is ServerVariableInterface.ServerVariableInline ->
                    serverVariableValidator.validate(serverVariableInterface.serverVariable, contextString, results)

                is ServerVariableInterface.ServerVariableReference -> {
                    referenceResolver.resolve(serverVariableInterface.reference, contextString, results)
                }
            }
        }
    }

    private fun validateCorrelationIds(component: Component, results: ValidationResults) {
        component.correlationIds?.forEach { (correlationIdName, correlationIdInterface) ->
            val contextString = "Component Correlation ID '$correlationIdName'"
            correlationIdValidator.validateInterface(correlationIdInterface, contextString, results)
        }
    }

    private fun validateReplies(component: Component, results: ValidationResults) {
        component.replies?.forEach { (operationReplyName, operationReplyInterface) ->
            val contextString = "Component Operation Reply '$operationReplyName'"
            operationReplyValidator.validateInterface(operationReplyInterface, contextString, results)
        }
    }

    private fun validateChannels(component: Component, results: ValidationResults) {
        component.channels?.forEach { (channelName, channelInterface) ->
            val contextString = "Component Channel '$channelName'"
            channelValidator.validateInterface(channelInterface, contextString, results)
        }
    }

    private fun validateExternalDocs(component: Component, results: ValidationResults) {
        component.externalDocs?.forEach { (externalDocName, externalDocInterface) ->
            val contextString = "Component External Doc '$externalDocName'"
            externalDocsValidator.validateInterface(externalDocInterface, contextString, results)
        }
    }

    private fun validateTags(component: Component, results: ValidationResults) {
        component.tags?.let { tagValidator.validateMap(it, results) }
    }

    private fun validateOperationReplyAddresses(component: Component, results: ValidationResults) {
        component.replyAddresses?.forEach { (operationReplyAddressName, operationReplyAddressInterface) ->
            val contextString = "Component Operation Reply Address '$operationReplyAddressName'"
            operationReplyAddressValidator.validateInterface(operationReplyAddressInterface, contextString, results)
        }
    }

    private fun validateOperationTraits(component: Component, results: ValidationResults) {
        component.operationTraits?.forEach { (operationTraitName, operationTraitInterface) ->
            val contextString = "Component Operation Trait '$operationTraitName'"
            operationTraitValidator.validateInterface(operationTraitInterface, contextString, results)
        }
    }

    private fun validateMessageTraits(component: Component, results: ValidationResults) {
        component.messageTraits?.forEach { (messageTraitName, messageTraitInterface) ->
            val contextString = "Component Message Trait '$messageTraitName'"
            messageTraitValidator.validateInterface(messageTraitInterface, contextString, results)
        }
    }

    private fun validateBindings(component: Component, results: ValidationResults) {
        validateBindingMap(component.serverBindings, "Component Server", results)
        validateBindingMap(component.channelBindings, "Component Channel", results)
        validateBindingMap(component.operationBindings, "Component Operation", results)
        validateBindingMap(component.messageBindings, "Component Message", results)
    }

    private fun validateBindingMap(
        bindings: Map<String, BindingInterface>?,
        contextString: String,
        results: ValidationResults,
    ) {
        bindings?.forEach { (bindingName, bindingInterface) ->
            val contextString = "$contextString Binding '$bindingName'"
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, contextString, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, contextString, results)
            }
        }
    }
}
