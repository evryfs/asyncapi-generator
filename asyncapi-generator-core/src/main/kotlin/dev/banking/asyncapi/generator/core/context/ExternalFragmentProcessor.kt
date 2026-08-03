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
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.UNKNOWN
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
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidationProfile
import dev.banking.asyncapi.generator.core.validator.ReferenceTargetTraversal
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
import dev.banking.asyncapi.generator.core.validator.util.ValidationReporter

internal class ExternalFragmentProcessor(
    private val context: AsyncApiContext,
) {
    internal fun parseAndDeferValidation(
        target: ExternalReferenceTargetResolver.Target,
        reference: Reference,
    ): () -> Unit {
        val category = reference.referenceCategoryKey
            ?: throw IllegalArgumentException("Missing referenceCategoryKey for ref '${reference.ref}'")
        val targetNode = target.node
        return when (category) {
            SCHEMA -> parseSchema(targetNode)
            CHANNEL -> parseChannel(targetNode)
            MESSAGE -> parseMessage(targetNode)
            MESSAGE_TRAIT -> parseMessageTrait(targetNode)
            OPERATION -> parseOperation(targetNode)
            OPERATION_TRAIT -> parseOperationTrait(targetNode)
            OPERATION_REPLY -> parseOperationReply(targetNode)
            OPERATION_REPLY_ADDRESS -> parseOperationReplyAddress(targetNode)
            SERVER -> parseServer(targetNode)
            SERVER_VARIABLE -> parseServerVariable(targetNode)
            PARAMETER -> parseParameter(targetNode)
            SECURITY_SCHEME -> parseSecurityScheme(targetNode)
            CORRELATION_ID -> parseCorrelationId(targetNode)
            EXTERNAL_DOC -> parseExternalDoc(targetNode)
            TAG -> parseTag(targetNode)
            BINDING -> parseBinding(targetNode, reference)
            REFERENCE -> throw IllegalArgumentException(
                "Generic reference category 'REFERENCE' is not supported for external fragment parsing: '${reference.ref}'. " +
                    "Assign a concrete ReferenceCategoryKey at parser creation site."
            )
        }
    }

    private fun parseSchema(targetNode: ParserNode): () -> Unit {
        val selectedSchema = SchemaParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            SchemaValidator(context).validateInterface(
                selectedSchema,
                "External Schema '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseChannel(targetNode: ParserNode): () -> Unit {
        val parsed: ChannelInterface = ChannelParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            ChannelValidator(context).validateInterface(
                parsed,
                "External Channel '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseMessage(targetNode: ParserNode): () -> Unit {
        val parsed: MessageInterface = MessageParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            val validationContext = "External Message '${targetNode.name}'"
            when (parsed) {
                is MessageInterface.MessageInline ->
                    MessageValidator(context).validate(parsed.message, validationContext, results)

                is MessageInterface.MessageReference ->
                    ReferenceResolver(context).resolve(parsed.reference, MESSAGE, validationContext, results)
            }
        }
    }

    private fun parseMessageTrait(targetNode: ParserNode): () -> Unit {
        val parsed: MessageTraitInterface = MessageTraitParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            MessageTraitValidator(context).validateInterface(
                parsed,
                "External MessageTrait '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseOperation(targetNode: ParserNode): () -> Unit {
        val parsed: OperationInterface = OperationParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            OperationValidator(context).validateInterface(
                parsed,
                "External Operation '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseOperationTrait(targetNode: ParserNode): () -> Unit {
        val parsed: OperationTraitInterface = OperationTraitParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            OperationTraitValidator(context).validateInterface(
                parsed,
                "External OperationTrait '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseOperationReply(targetNode: ParserNode): () -> Unit {
        val parsed: OperationReplyInterface = OperationReplyParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            OperationReplyValidator(context).validateInterface(
                parsed,
                "External OperationReply '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseOperationReplyAddress(targetNode: ParserNode): () -> Unit {
        val parsed: OperationReplyAddressInterface = OperationReplyAddressParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            OperationReplyAddressValidator(context).validateInterface(
                parsed,
                "External OperationReplyAddress '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseServer(targetNode: ParserNode): () -> Unit {
        val parsed: ServerInterface = ServerParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            ServerValidator(context).validateInterface(
                parsed,
                "External Server '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseServerVariable(targetNode: ParserNode): () -> Unit {
        val parsed: ServerVariableInterface = ServerVariableParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            val validationContext = "External ServerVariable '${targetNode.name}'"
            when (parsed) {
                is ServerVariableInterface.ServerVariableInline ->
                    ServerVariableValidator(context).validate(parsed.serverVariable, validationContext, results)

                is ServerVariableInterface.ServerVariableReference ->
                    ReferenceResolver(context).resolve(
                        parsed.reference,
                        SERVER_VARIABLE,
                        validationContext,
                        results,
                    )
            }
        }
    }

    private fun parseParameter(targetNode: ParserNode): () -> Unit {
        val parsed: ParameterInterface = ParameterParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            ParameterValidator(context).validateInterface(
                parsed,
                "External Parameter '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseSecurityScheme(targetNode: ParserNode): () -> Unit {
        val parsed: SecuritySchemeInterface = SecuritySchemeParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            val validationContext = "External SecurityScheme '${targetNode.name}'"
            when (parsed) {
                is SecuritySchemeInterface.SecuritySchemeInline ->
                    SecuritySchemeValidator(context).validate(parsed.security, validationContext, results)

                is SecuritySchemeInterface.SecuritySchemeReference ->
                    ReferenceResolver(context).resolve(
                        parsed.reference,
                        SECURITY_SCHEME,
                        validationContext,
                        results,
                    )
            }
        }
    }

    private fun parseCorrelationId(targetNode: ParserNode): () -> Unit {
        val parsed: CorrelationIdInterface = CorrelationIdParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            CorrelationIdValidator(context).validateInterface(
                parsed,
                "External CorrelationId '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseExternalDoc(targetNode: ParserNode): () -> Unit {
        val parsed: ExternalDocInterface = ExternalDocsParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            ExternalDocsValidator(context).validateInterface(
                parsed,
                "External ExternalDoc '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseTag(targetNode: ParserNode): () -> Unit {
        val parsed: TagInterface = TagParser(context).parseElement(targetNode)
        return deferredValidation { results ->
            TagValidator(context).validateInterface(
                parsed,
                "External Tag '${targetNode.name}'",
                results,
            )
        }
    }

    private fun parseBinding(targetNode: ParserNode, reference: Reference): () -> Unit {
        val origin = context.getBindingReferenceOrigin(reference)
        val parsed: BindingInterface = if (origin?.protocol != null) {
            BindingParser(context).parseProtocol(targetNode, origin.location, origin.protocol)
        } else {
            BindingParser(context).parseComponent(targetNode, origin?.location ?: UNKNOWN)
        }
        return deferredValidation { results ->
            val validationContext = "External Binding '${targetNode.name}'"
            when (parsed) {
                is BindingInterface.BindingInline ->
                    BindingValidator(context).validate(parsed.binding, validationContext, results)

                is BindingInterface.BindingReference ->
                    ReferenceResolver(context).resolve(parsed.reference, BINDING, validationContext, results)
            }
        }
    }

    private fun deferredValidation(validate: (ValidationCollector) -> Unit): () -> Unit = {
        val results = ValidationCollector(AsyncApiValidationProfile.V3_0)
        validate(results)
        ReferenceTargetTraversal(context).drain(results)
        val report = results.report()
        ValidationReporter(context).throwErrors(report)
        context.collectExternalValidationWarnings(report.warnings)
    }
}
