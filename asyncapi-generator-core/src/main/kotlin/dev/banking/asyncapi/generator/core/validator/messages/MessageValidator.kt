package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE_TRAIT
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_CONTENT_TYPE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_HEADER_FORMAT_UNSUPPORTED
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.correlations.CorrelationIdValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats

internal class MessageValidator(
    val asyncApiContext: AsyncApiContext,
) {
    private val tagValidator = TagValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val schemaValidator = SchemaValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val messageTraitValidator = MessageTraitValidator(asyncApiContext)
    private val correlationIdValidator = CorrelationIdValidator(asyncApiContext)
    private val messageExampleValidator = MessageExampleValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validate(message: Message, contextString: String, results: ValidationCollector) {
        if (!results.visit(message)) return
        validatePayload(message, contextString, results)
        validateHeaders(message, contextString, results)
        validateContentType(message, contextString, results)
        validateExamples(message, contextString, results)
        validateCorrelationId(message, contextString, results)
        validateTraits(message, contextString, results)
        validateTags(message, contextString, results)
        validateExternalDocs(message, contextString, results)
        validateBindings(message, contextString, results)
    }

    private fun validateContentType(node: Message, contextString: String, results: ValidationCollector) {
        val contentType = node.contentType ?: return
        if (!ValidationFormats.isSpecificMediaType(contentType)) {
            results.error(
                MESSAGE_CONTENT_TYPE_FORMAT,
                "$contextString has invalid 'contentType' value '$contentType'. Expected a specific media type.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::contentType),
            )
        }
    }

    private fun validateExamples(node: Message, contextString: String, results: ValidationCollector) {
        node.examples?.let { examples ->
            messageExampleValidator.validate(
                examples = examples,
                headersSchema = node.headers,
                payloadSchema = node.payload,
                contextString = contextString,
                results = results,
            )
        }
    }

    private fun validatePayload(node: Message, contextString: String, results: ValidationCollector) {
        val contextString = "$contextString Payload"
        node.payload?.let { payload -> schemaValidator.validateInterface(payload, contextString, results) }
    }

    private fun validateHeaders(node: Message, contextString: String, results: ValidationCollector) {
        val headersSchema = node.headers ?: return
        schemaValidator.validateInterface(headersSchema, "$contextString Headers", results)

        if (headersSchema is SchemaInterface.SchemaInline) {
            headersSchema.schema.properties
                ?.filterValues { schema -> schema is SchemaInterface.MultiFormatSchemaInline }
                ?.forEach { (headerName, _) ->
                    results.warn(
                        MESSAGE_HEADER_FORMAT_UNSUPPORTED,
                        "$contextString Header '$headerName' uses a Multi Format Schema, which is not validated " +
                            "in message headers.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, node::headers),
                    )
                }
        }
    }

    private fun validateCorrelationId(node: Message, contextString: String, results: ValidationCollector) {
        node.correlationId?.let { correlationId ->
            correlationIdValidator.validateInterface(correlationId, "$contextString Correlation ID", results)
        }
    }

    private fun validateTraits(node: Message, contextString: String, results: ValidationCollector) {
        val traits = node.traits ?: return
        if (traits.isEmpty()) return
        traits.forEachIndexed { index, trait ->
            val contextString = "$contextString Trait[$index]"
            when (trait) {
                is MessageTraitInterface.InlineMessageTrait ->
                    messageTraitValidator.validate(trait.trait, contextString, results)

                is MessageTraitInterface.ReferenceMessageTrait ->
                    referenceResolver.resolve(trait.reference, MESSAGE_TRAIT, contextString, results)
            }
        }
    }

    private fun validateTags(node: Message, contextString: String, results: ValidationCollector) {
        val tags = node.tags ?: return
        tags.forEachIndexed { index, tagInterface ->
            val contextString = "$contextString Tag[$index]"
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, contextString, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(tagInterface.reference, TAG, contextString, results)
            }
        }
    }

    private fun validateExternalDocs(node: Message, contextString: String, results: ValidationCollector) {
        val contextString = "$contextString ExternalDocs"
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(docs.reference, EXTERNAL_DOC, contextString, results)

            null -> {}
        }
    }

    private fun validateBindings(node: Message, contextString: String, results: ValidationCollector) {
        val bindings = node.bindings ?: return
        if (bindings.isEmpty()) return
        bindings.forEach { (bindingName, bindingInterface) ->
            val contextString = "$contextString Binding '$bindingName'"
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, contextString, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, BINDING, contextString, results)
            }
        }
    }

}
