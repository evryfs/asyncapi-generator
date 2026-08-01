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
import dev.banking.asyncapi.generator.core.document.DocumentMember
import dev.banking.asyncapi.generator.core.document.DocumentObject
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
        val targetMap = singleTargetMap(target.node)
        when (category) {
            SCHEMA -> parseAndValidateSchemas(target, reference, results)
            CHANNEL -> parseAndValidateChannels(targetMap, results)
            MESSAGE -> parseAndValidateMessage(target.node, results)
            MESSAGE_TRAIT -> parseAndValidateMessageTraits(targetMap, results)
            OPERATION -> parseAndValidateOperations(targetMap, results)
            OPERATION_TRAIT -> parseAndValidateOperationTraits(targetMap, results)
            OPERATION_REPLY -> parseAndValidateOperationReplies(targetMap, results)
            OPERATION_REPLY_ADDRESS -> parseAndValidateOperationReplyAddresses(targetMap, results)
            SERVER -> parseAndValidateServers(targetMap, results)
            SERVER_VARIABLE -> parseAndValidateServerVariables(targetMap, results)
            PARAMETER -> parseAndValidateParameters(targetMap, results)
            SECURITY_SCHEME -> parseAndValidateSecuritySchemes(targetMap, results)
            CORRELATION_ID -> parseAndValidateCorrelationIds(targetMap, results)
            EXTERNAL_DOC -> parseAndValidateExternalDocs(targetMap, results)
            TAG -> parseAndValidateTags(targetMap, results)
            BINDING -> parseAndValidateBindings(targetMap, results)
            else -> { /* Should not happen */ }
        }
        results.logWarnings()
        results.throwErrors()
    }

    private fun parseAndValidateSchemas(
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

    private fun parseAndValidateChannels(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, ChannelInterface> = ChannelParser(context).parseMap(rootNode)
        val validator = ChannelValidator(context)
        parsed.forEach { (name, channel) ->
            validator.validateInterface(channel, "External Channel '$name'", results)
        }
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

    private fun parseAndValidateMessageTraits(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, MessageTraitInterface> = MessageTraitParser(context).parseMap(rootNode)
        val validator = MessageTraitValidator(context)
        parsed.forEach { (name, trait) ->
            validator.validateInterface(trait, "External MessageTrait '$name'", results)
        }
    }

    private fun parseAndValidateOperations(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, OperationInterface> = OperationParser(context).parseMap(rootNode)
        val validator = OperationValidator(context)
        parsed.forEach { (name, op) ->
            validator.validateInterface(op, "External Operation '$name'", results)
        }
    }

    private fun parseAndValidateOperationTraits(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, OperationTraitInterface> = OperationTraitParser(context).parseMap(rootNode)
        val validator = OperationTraitValidator(context)
        parsed.forEach { (name, trait) ->
            validator.validateInterface(trait, "External OperationTrait '$name'", results)
        }
    }

    private fun parseAndValidateOperationReplies(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, OperationReplyInterface> = OperationReplyParser(context).parseMap(rootNode)
        val validator = OperationReplyValidator(context)
        parsed.forEach { (name, reply) ->
            validator.validateInterface(reply, "External OperationReply '$name'", results)
        }
    }

    private fun parseAndValidateOperationReplyAddresses(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, OperationReplyAddressInterface> =
            OperationReplyAddressParser(context).parseMap(rootNode)
        val validator = OperationReplyAddressValidator(context)
        parsed.forEach { (name, addr) ->
            validator.validateInterface(addr, "External OperationReplyAddress '$name'", results)
        }
    }

    private fun parseAndValidateServers(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, ServerInterface> = ServerParser(context).parseMap(rootNode)
        val validator = ServerValidator(context)
        parsed.forEach { (name, server) ->
            validator.validateInterface(server, "External Server '$name'", results)
        }
    }

    private fun parseAndValidateServerVariables(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, ServerVariableInterface> = ServerVariableParser(context).parseMap(rootNode)
        val validator = ServerVariableValidator(context)
        val resolver = ReferenceResolver(context)
        parsed.forEach { (name, variable) ->
            val ctx = "External ServerVariable '$name'"
            when (variable) {
                is ServerVariableInterface.ServerVariableInline -> validator.validate(
                    variable.serverVariable,
                    ctx,
                    results
                )

                is ServerVariableInterface.ServerVariableReference -> resolver.resolve(variable.reference, ctx, results)
            }
        }
    }

    private fun parseAndValidateParameters(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, ParameterInterface> = ParameterParser(context).parseMap(rootNode)
        val validator = ParameterValidator(context)
        parsed.forEach { (name, parameter) ->
            validator.validateInterface(parameter, "External Parameter '$name'", results)
        }
    }

    private fun parseAndValidateSecuritySchemes(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, SecuritySchemeInterface> = SecuritySchemeParser(context).parseMap(rootNode)
        val validator = SecuritySchemeValidator(context)
        val resolver = ReferenceResolver(context)
        parsed.forEach { (name, schemeInterface) ->
            val ctx = "External SecurityScheme '$name'"
            when (schemeInterface) {
                is SecuritySchemeInterface.SecuritySchemeInline -> validator.validate(
                    schemeInterface.security,
                    ctx,
                    results
                )

                is SecuritySchemeInterface.SecuritySchemeReference -> resolver.resolve(
                    schemeInterface.reference,
                    ctx,
                    results
                )
            }
        }
    }

    private fun parseAndValidateCorrelationIds(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, CorrelationIdInterface> = CorrelationIdParser(context).parseMap(rootNode)
        val validator = CorrelationIdValidator(context)
        parsed.forEach { (name, correlationId) ->
            validator.validateInterface(correlationId, "External CorrelationId '$name'", results)
        }
    }

    private fun parseAndValidateExternalDocs(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, ExternalDocInterface> = ExternalDocsParser(context).parseMap(rootNode)
        val validator = ExternalDocsValidator(context)
        parsed.forEach { (name, extDoc) ->
            validator.validateInterface(extDoc, "External ExternalDoc '$name'", results)
        }
    }

    private fun parseAndValidateTags(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, TagInterface> = TagParser(context).parseMap(rootNode)
        val validator = TagValidator(context)
        parsed.forEach { (name, tag) ->
            validator.validateInterface(tag, "External Tag '$name'", results)
        }
    }

    private fun parseAndValidateBindings(rootNode: ParserNode, results: ValidationResults) {
        val parsed: Map<String, BindingInterface> = BindingParser(context).parseMap(rootNode)
        val validator = BindingValidator(context)
        val resolver = ReferenceResolver(context)
        parsed.forEach { (name, bindingInterface) ->
            val ctx = "External Binding '$name'"
            when (bindingInterface) {
                is BindingInterface.BindingInline -> validator.validate(bindingInterface.binding, ctx, results)
                is BindingInterface.BindingReference -> resolver.resolve(bindingInterface.reference, ctx, results)
            }
        }
    }

    private fun singleTargetMap(target: ParserNode): ParserNode {
        val memberName =
            if (target.path == target.name) {
                target.path.substringAfterLast('.')
            } else {
                target.name
            }
        val parentPath = target.path.removeSuffix(".$memberName")
        val targetObject = DocumentObject(
            members = mapOf(
                memberName to DocumentMember(
                    keyLocation = target.node.location,
                    value = target.node,
                ),
            ),
            location = target.node.location,
        )
        return ParserNode(
            name = parentPath,
            node = targetObject,
            path = parentPath,
            context = target.context,
            profile = target.profile,
        )
    }

}
