package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.*
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.channels.ChannelParser
import dev.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.messages.MessageParser
import dev.banking.asyncapi.generator.core.parser.messages.MessageTraitParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.operations.OperationParser
import dev.banking.asyncapi.generator.core.parser.operations.OperationReplyAddressParser
import dev.banking.asyncapi.generator.core.parser.operations.OperationReplyParser
import dev.banking.asyncapi.generator.core.parser.operations.OperationTraitParser
import dev.banking.asyncapi.generator.core.parser.parameters.ParameterParser
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import dev.banking.asyncapi.generator.core.parser.servers.ServerParser
import dev.banking.asyncapi.generator.core.parser.servers.ServerVariableParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
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

class ExternalFragmentProcessor(
    private val context: AsyncApiContext,
) {
    internal fun parseAndValidate(
        target: ExternalReferenceTargetResolver.Target,
        reference: Reference,
    ) {
        val category = reference.referenceCategoryKey
            ?: throw IllegalArgumentException("Missing referenceCategoryKey for ref '${reference.ref}'")
        if (category == REFERENCE) {
            throw IllegalArgumentException(
                "Generic reference category 'REFERENCE' is not supported for external fragment parsing: '${reference.ref}'. " +
                    "Assign a concrete ReferenceCategoryKey at parser creation site."
            )
        }
        val results = ValidationResults(context)
        val targetNode = target.node
        when (category) {
            SCHEMA -> parseAndValidateSchema(target, reference, results)
            CHANNEL -> parseAndValidateChannel(targetNode, results)
            MESSAGE -> parseAndValidateMessage(targetNode, results)
            MESSAGE_TRAIT -> parseAndValidateMessageTrait(targetNode, results)
            OPERATION -> parseAndValidateOperation(targetNode, results)
            OPERATION_TRAIT -> parseAndValidateOperationTrait(targetNode, results)
            OPERATION_REPLY -> parseAndValidateOperationReply(targetNode, results)
            OPERATION_REPLY_ADDRESS -> parseAndValidateOperationReplyAddress(targetNode, results)
            SERVER -> parseAndValidateServer(targetNode, results)
            SERVER_VARIABLE -> parseAndValidateServerVariable(targetNode, results)
            PARAMETER -> parseAndValidateParameter(targetNode, results)
            SECURITY_SCHEME -> parseAndValidateSecurityScheme(targetNode, results)
            CORRELATION_ID -> parseAndValidateCorrelationId(targetNode, results)
            EXTERNAL_DOC -> parseAndValidateExternalDoc(targetNode, results)
            TAG -> parseAndValidateTag(targetNode, results)
            BINDING -> parseAndValidateBinding(targetNode, results)
            else -> { /* Should not happen */ }
        }
        results.logWarnings()
        results.throwErrors()
    }

    private fun parseAndValidateSchema(
        target: ExternalReferenceTargetResolver.Target,
        reference: Reference,
        results: ValidationResults,
    ) {
        val selectedSchema =
            target.objectContainer?.let { container ->
                SchemaParser(context).parseMap(container)[target.node.name]
            } ?: SchemaParser(context).parseElement(target.node)
        requireNotNull(selectedSchema) {
            "Resolved schema target '${reference.ref}' was not produced by SchemaParser"
        }
        val validator = SchemaValidator(context)
        validator.validateInterface(
            selectedSchema,
            "External Schema '${target.node.name}'",
            results,
        )
    }

    private fun parseAndValidateChannel(targetNode: ParserNode, results: ValidationResults) {
        val parsed: ChannelInterface = ChannelParser(context).parseElement(targetNode)
        ChannelValidator(context).validateInterface(
            parsed,
            "External Channel '${targetNode.name}'",
            results,
        )
    }

    private fun parseAndValidateMessage(targetNode: ParserNode, results: ValidationResults) {
        val parsed: MessageInterface = MessageParser(context).parseElement(targetNode)
        val validator = MessageValidator(context)
        val resolver = ReferenceResolver(context)
        val validationContext = "External Message '${targetNode.name}'"
        when (parsed) {
            is MessageInterface.MessageInline ->
                validator.validate(parsed.message, validationContext, results)

            is MessageInterface.MessageReference ->
                resolver.resolve(parsed.reference, validationContext, results)
        }
    }

    private fun parseAndValidateMessageTrait(targetNode: ParserNode, results: ValidationResults) {
        val parsed: MessageTraitInterface = MessageTraitParser(context).parseElement(targetNode)
        MessageTraitValidator(context).validateInterface(
            parsed,
            "External MessageTrait '${targetNode.name}'",
            results,
        )
    }

    private fun parseAndValidateOperation(targetNode: ParserNode, results: ValidationResults) {
        val parsed: OperationInterface = OperationParser(context).parseElement(targetNode)
        OperationValidator(context).validateInterface(
            parsed,
            "External Operation '${targetNode.name}'",
            results,
        )
    }

    private fun parseAndValidateOperationTrait(targetNode: ParserNode, results: ValidationResults) {
        val parsed: OperationTraitInterface = OperationTraitParser(context).parseElement(targetNode)
        OperationTraitValidator(context).validateInterface(
            parsed,
            "External OperationTrait '${targetNode.name}'",
            results,
        )
    }

    private fun parseAndValidateOperationReply(targetNode: ParserNode, results: ValidationResults) {
        val parsed: OperationReplyInterface = OperationReplyParser(context).parseElement(targetNode)
        OperationReplyValidator(context).validateInterface(
            parsed,
            "External OperationReply '${targetNode.name}'",
            results,
        )
    }

    private fun parseAndValidateOperationReplyAddress(targetNode: ParserNode, results: ValidationResults) {
        val parsed: OperationReplyAddressInterface = OperationReplyAddressParser(context).parseElement(targetNode)
        OperationReplyAddressValidator(context).validateInterface(
            parsed,
            "External OperationReplyAddress '${targetNode.name}'",
            results,
        )
    }

    private fun parseAndValidateServer(targetNode: ParserNode, results: ValidationResults) {
        val parsed: ServerInterface = ServerParser(context).parseElement(targetNode)
        ServerValidator(context).validateInterface(
            parsed,
            "External Server '${targetNode.name}'",
            results,
        )
    }

    private fun parseAndValidateServerVariable(targetNode: ParserNode, results: ValidationResults) {
        val parsed: ServerVariableInterface = ServerVariableParser(context).parseElement(targetNode)
        val validator = ServerVariableValidator(context)
        val resolver = ReferenceResolver(context)
        val validationContext = "External ServerVariable '${targetNode.name}'"
        when (parsed) {
            is ServerVariableInterface.ServerVariableInline ->
                validator.validate(parsed.serverVariable, validationContext, results)

            is ServerVariableInterface.ServerVariableReference ->
                resolver.resolve(parsed.reference, validationContext, results)
        }
    }

    private fun parseAndValidateParameter(targetNode: ParserNode, results: ValidationResults) {
        val parsed: ParameterInterface = ParameterParser(context).parseElement(targetNode)
        ParameterValidator(context).validateInterface(
            parsed,
            "External Parameter '${targetNode.name}'",
            results,
        )
    }

    private fun parseAndValidateSecurityScheme(targetNode: ParserNode, results: ValidationResults) {
        val parsed: SecuritySchemeInterface = SecuritySchemeParser(context).parseElement(targetNode)
        val validator = SecuritySchemeValidator(context)
        val resolver = ReferenceResolver(context)
        val validationContext = "External SecurityScheme '${targetNode.name}'"
        when (parsed) {
            is SecuritySchemeInterface.SecuritySchemeInline ->
                validator.validate(parsed.security, validationContext, results)

            is SecuritySchemeInterface.SecuritySchemeReference ->
                resolver.resolve(parsed.reference, validationContext, results)
        }
    }

    private fun parseAndValidateCorrelationId(targetNode: ParserNode, results: ValidationResults) {
        val parsed: CorrelationIdInterface = CorrelationIdParser(context).parseElement(targetNode)
        CorrelationIdValidator(context).validateInterface(
            parsed,
            "External CorrelationId '${targetNode.name}'",
            results,
        )
    }

    private fun parseAndValidateExternalDoc(targetNode: ParserNode, results: ValidationResults) {
        val parsed: ExternalDocInterface = ExternalDocsParser(context).parseElement(targetNode)
        ExternalDocsValidator(context).validateInterface(
            parsed,
            "External ExternalDoc '${targetNode.name}'",
            results,
        )
    }

    private fun parseAndValidateTag(targetNode: ParserNode, results: ValidationResults) {
        val parsed: TagInterface = TagParser(context).parseElement(targetNode)
        TagValidator(context).validateInterface(
            parsed,
            "External Tag '${targetNode.name}'",
            results,
        )
    }

    private fun parseAndValidateBinding(targetNode: ParserNode, results: ValidationResults) {
        val parsed: BindingInterface = BindingParser(context).parseElement(targetNode)
        val validator = BindingValidator(context)
        val resolver = ReferenceResolver(context)
        val validationContext = "External Binding '${targetNode.name}'"
        when (parsed) {
            is BindingInterface.BindingInline ->
                validator.validate(parsed.binding, validationContext, results)

            is BindingInterface.BindingReference ->
                resolver.resolve(parsed.reference, validationContext, results)
        }
    }
}
