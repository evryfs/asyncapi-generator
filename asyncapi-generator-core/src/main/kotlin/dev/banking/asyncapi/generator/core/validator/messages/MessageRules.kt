package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CORRELATION_ID
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE_TRAIT
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_CONTENT_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_HEADER_FORMAT
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Message Object and walks its payload, headers, examples, correlation ID, traits, tags,
 * external documentation and bindings.
 *
 * @param asyncApiContext the load the messages belong to
 */
internal class MessageRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<Message> {
    private val messageExampleValidator = MessageExampleValidator(asyncApiContext)

    override fun check(
        node: Message,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        node.payload?.let { walk.schema(it, path.child("Payload")) }
        validateHeaders(node, path, walk)
        validateContentType(node, path, walk)
        validateExamples(node, path, walk)
        node.correlationId?.let { walk.member(it, CORRELATION_ID, path.child("Correlation ID")) }
        node.traits?.forEachIndexed { index, trait ->
            walk.member(trait, MESSAGE_TRAIT, path.indexed("Trait", index))
        }
        walk.tags(node.tags, path)
        walk.externalDocs(node.externalDocs, path)
        walk.bindings(node.bindings, path)
    }

    private fun validateContentType(
        node: Message,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val contentType = node.contentType ?: return
        if (!ValidationFormats.isSpecificMediaType(contentType)) {
            walk.results.error(
                MESSAGE_CONTENT_TYPE,
                "$path has invalid 'contentType' value '$contentType'. Expected a specific media type.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::contentType),
            )
        }
    }

    private fun validateExamples(
        node: Message,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        node.examples?.let { examples ->
            messageExampleValidator.validate(
                examples = examples,
                headersSchema = node.headers,
                payloadSchema = node.payload,
                path = path,
                results = walk.results,
            )
        }
    }

    private fun validateHeaders(
        node: Message,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val headersSchema = node.headers ?: return
        walk.schema(headersSchema, path.child("Headers"))

        if (headersSchema is SchemaInterface.SchemaInline) {
            headersSchema.schema.properties
                ?.filterValues { schema -> schema is SchemaInterface.MultiFormatSchemaInline }
                ?.forEach { (headerName, _) ->
                    walk.results.warn(
                        MESSAGE_HEADER_FORMAT,
                        "$path Header '$headerName' uses a Multi Format Schema, which is not validated " +
                            "in message headers.",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::headers),
                    )
                }
        }
    }
}
