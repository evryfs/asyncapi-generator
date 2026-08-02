package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.*
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.tags.Tag
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

/** Drains reachable, type-checked reference targets through their ordinary domain validators. */
internal class ReferenceTargetTraversal(context: AsyncApiContext) {
    private val schemaValidator = SchemaValidator(context)
    private val channelValidator = ChannelValidator(context)
    private val messageValidator = MessageValidator(context)
    private val messageTraitValidator = MessageTraitValidator(context)
    private val operationValidator = OperationValidator(context)
    private val operationTraitValidator = OperationTraitValidator(context)
    private val operationReplyValidator = OperationReplyValidator(context)
    private val operationReplyAddressValidator = OperationReplyAddressValidator(context)
    private val serverValidator = ServerValidator(context)
    private val serverVariableValidator = ServerVariableValidator(context)
    private val parameterValidator = ParameterValidator(context)
    private val securitySchemeValidator = SecuritySchemeValidator(context)
    private val correlationIdValidator = CorrelationIdValidator(context)
    private val externalDocsValidator = ExternalDocsValidator(context)
    private val tagValidator = TagValidator(context)
    private val bindingValidator = BindingValidator(context)

    fun drain(results: ValidationCollector) {
        while (true) {
            val pending = results.nextReferenceTarget() ?: return
            val target = pending.target
            val contextString = "${pending.contextString} target"
            when (pending.category) {
                SCHEMA -> validateSchema(target, contextString, results)
                CHANNEL -> (target as? Channel)?.let {
                    channelValidator.validateInterface(ChannelInterface.ChannelInline(it), contextString, results)
                }
                MESSAGE -> (target as? Message)?.let { messageValidator.validate(it, contextString, results) }
                MESSAGE_TRAIT ->
                    (target as? MessageTrait)?.let { messageTraitValidator.validate(it, contextString, results) }
                OPERATION -> (target as? Operation)?.let {
                    operationValidator.validateInterface(OperationInterface.OperationInline(it), contextString, results)
                }
                OPERATION_TRAIT ->
                    (target as? OperationTrait)?.let { operationTraitValidator.validate(it, contextString, results) }
                OPERATION_REPLY ->
                    (target as? OperationReply)?.let { operationReplyValidator.validate(it, contextString, results) }
                OPERATION_REPLY_ADDRESS ->
                    (target as? OperationReplyAddress)?.let {
                        operationReplyAddressValidator.validate(it, contextString, results)
                    }
                SERVER -> (target as? Server)?.let {
                    serverValidator.validateInterface(ServerInterface.ServerInline(it), contextString, results)
                }
                SERVER_VARIABLE ->
                    (target as? ServerVariable)?.let { serverVariableValidator.validate(it, contextString, results) }
                PARAMETER -> (target as? Parameter)?.let { parameterValidator.validate(it, contextString, results) }
                SECURITY_SCHEME ->
                    (target as? SecurityScheme)?.let { securitySchemeValidator.validate(it, contextString, results) }
                CORRELATION_ID ->
                    (target as? CorrelationId)?.let { correlationIdValidator.validate(it, contextString, results) }
                EXTERNAL_DOC ->
                    (target as? ExternalDoc)?.let { externalDocsValidator.validate(it, contextString, results) }
                TAG -> (target as? Tag)?.let { tagValidator.validate(it, contextString, results) }
                BINDING -> (target as? Binding)?.let { bindingValidator.validate(it, contextString, results) }
                REFERENCE -> Unit
            }
        }
    }

    private fun validateSchema(target: Any, contextString: String, results: ValidationCollector) {
        when (target) {
            is Schema -> schemaValidator.validate(target, contextString, results)
            is MultiFormatSchema ->
                schemaValidator.validateInterface(
                    SchemaInterface.MultiFormatSchemaInline(target),
                    contextString,
                    results,
                )
            is SchemaInterface.BooleanSchema -> schemaValidator.validateInterface(target, contextString, results)
        }
    }
}
