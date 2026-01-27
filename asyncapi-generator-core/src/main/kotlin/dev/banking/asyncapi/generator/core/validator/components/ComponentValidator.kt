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
            when (schemaInterface) {
                is SchemaInterface.SchemaInline ->
                    schemaValidator.validate(schemaInterface.schema, schemaName, results)

                is SchemaInterface.MultiFormatSchemaInline ->
                    results.warn(
                        "Multi-format schemas are not yet validated (field '$schemaName').",
                        asyncApiContext.getLine(component, component::schemas)
                    )

                is SchemaInterface.SchemaReference ->
                    referenceResolver.resolve(schemaInterface.reference, "Component Schema", results)

                is SchemaInterface.BooleanSchema -> {}
            }
        }
    }

    private fun validateMessages(component: Component, results: ValidationResults) {
        component.messages?.forEach { (messageName, msgInterface) ->
            when (msgInterface) {
                is MessageInterface.MessageInline ->
                    messageValidator.validate(msgInterface.message, messageName, results)

                is MessageInterface.MessageReference -> {
                    referenceResolver.resolve(msgInterface.reference, "Component Message", results)
                }
            }
        }
    }

    private fun validateParameters(component: Component, results: ValidationResults) {
        component.parameters?.forEach { (parameterName, parameterInterface) ->
            parameterValidator.validateInterface(parameterName, parameterInterface, results)
        }
    }

    private fun validateSecuritySchemes(component: Component, results: ValidationResults) {
        component.securitySchemes?.forEach { (securitySchemeName, securitySchemeInterface) ->
            when (securitySchemeInterface) {
                is SecuritySchemeInterface.SecuritySchemeInline ->
                    securitySchemeValidator.validate(securitySchemeInterface.security, securitySchemeName, results)

                is SecuritySchemeInterface.SecuritySchemeReference -> {
                    referenceResolver.resolve(
                        securitySchemeInterface.reference,
                        "Component SecurityScheme",
                        results
                    )
                }
            }
        }
    }

    private fun validateOperations(component: Component, results: ValidationResults) {
        component.operations?.let { operationValidator.validateOperations(it, results) }
    }

    private fun validateServers(component: Component, results: ValidationResults) {
        component.servers?.let { serverValidator.validateServers(it, results) }
    }

    private fun validateServerVariables(component: Component, results: ValidationResults) {
        component.serverVariables?.forEach { (serverVariableName, serverVariableInterface) ->
            when (serverVariableInterface) {
                is ServerVariableInterface.ServerVariableInline ->
                    serverVariableValidator.validate(
                        serverVariableName,
                        serverVariableInterface.serverVariable,
                        results
                    )

                is ServerVariableInterface.ServerVariableReference -> {
                    referenceResolver.resolve(
                        serverVariableInterface.reference,
                        "Component ServerVariable",
                        results
                    )
                }
            }
        }
    }

    private fun validateCorrelationIds(component: Component, results: ValidationResults) {
        component.correlationIds?.forEach { (correlationIdName, correlationIdInterface) ->
            correlationIdValidator.validateInterface(correlationIdName, correlationIdInterface, results)
        }
    }

    private fun validateReplies(component: Component, results: ValidationResults) {
        component.replies?.forEach { (operationReplyName, operationReplyInterface) ->
            operationReplyValidator.validateInterface(operationReplyName, operationReplyInterface, results)
        }
    }

    private fun validateChannels(component: Component, results: ValidationResults) {
        component.channels?.let { channelValidator.validateChannels(it, results) }
    }

    private fun validateExternalDocs(component: Component, results: ValidationResults) {
        component.externalDocs?.let { externalDocsValidator.validateMap(it, results) }
    }

    private fun validateTags(component: Component, results: ValidationResults) {
        component.tags?.let { tagValidator.validateMap(it, results) }
    }

    private fun validateOperationReplyAddresses(component: Component, results: ValidationResults) {
        component.replyAddresses?.forEach { (operationReplyAddressName, operationReplyAddressInterface) ->
            operationReplyAddressValidator.validateInterface(
                operationReplyAddressName,
                operationReplyAddressInterface,
                results
            )
        }
    }

    private fun validateOperationTraits(component: Component, results: ValidationResults) {
        component.operationTraits?.forEach { (operationTraitName, operationTraitInterface) ->
            operationTraitValidator.validateInterface(operationTraitName, operationTraitInterface, results)
        }
    }

    private fun validateMessageTraits(component: Component, results: ValidationResults) {
        component.messageTraits?.forEach { (messageTraitName, messageTraitInterface) ->
            messageTraitValidator.validateInterface(messageTraitName, messageTraitInterface, results)
        }
    }

    private fun validateBindings(component: Component, results: ValidationResults) {
        validateBindingMap(component.serverBindings, "Component Server Binding", results)
        validateBindingMap(component.channelBindings, "Component Channel Binding", results)
        validateBindingMap(component.operationBindings, "Component Operation Binding", results)
        validateBindingMap(component.messageBindings, "Component Message Binding", results)
    }

    private fun validateBindingMap(bindings: Map<String, BindingInterface>?, name: String, results: ValidationResults) {
        bindings?.forEach { (bindingName, bindingInterface) ->
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingName, bindingInterface.binding, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, name, results)
            }
        }
    }
}
