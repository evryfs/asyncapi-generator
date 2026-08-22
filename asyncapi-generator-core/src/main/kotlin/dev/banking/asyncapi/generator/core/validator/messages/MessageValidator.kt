package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
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

    fun validateInterface(node: MessageInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is MessageInterface.MessageInline ->
                validate(node.message, contextString, results)

            is MessageInterface.MessageReference ->
                referenceResolver.resolve(node.reference, MESSAGE, contextString, results)
        }
    }

    fun validate(node: Message, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validatePayload(node, contextString, results)
        validateHeaders(node, contextString, results)
        validateContentType(node, contextString, results)
        validateExamples(node, contextString, results)
        validateCorrelationId(node, contextString, results)
        validateTraits(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)
        validateBindings(node, contextString, results)
    }

    private fun validateContentType(node: Message, contextString: String, results: ValidationCollector) {
        val contentType = node.contentType ?: return
        if (!ValidationFormats.isSpecificMediaType(contentType)) {
            results.error(
                MESSAGE_CONTENT_TYPE_FORMAT,
                "$contextString has invalid 'contentType' value '$contentType'. Expected a specific media type.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::contentType),
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
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::headers),
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
        traits.forEachIndexed { index, trait ->
            val traitContext = "$contextString Trait[$index]"
            messageTraitValidator.validateInterface(trait, traitContext, results)
        }
    }

    private fun validateTags(node: Message, contextString: String, results: ValidationCollector) {
        tagValidator.validateList(node.tags, contextString, results)
    }

    private fun validateExternalDocs(node: Message, contextString: String, results: ValidationCollector) {
        val externalDocs = node.externalDocs ?: return
        val context = "$contextString ExternalDocs"
        externalDocsValidator.validateInterface(externalDocs, context, results)
    }

    private fun validateBindings(node: Message, contextString: String, results: ValidationCollector) {
        bindingValidator.validateMap(node.bindings, contextString, results)
    }

}
