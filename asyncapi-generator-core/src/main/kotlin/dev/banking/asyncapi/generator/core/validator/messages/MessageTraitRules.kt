package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CORRELATION_ID
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_CONTENT_TYPE
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Message Trait Object and walks its headers, examples, tags, external documentation,
 * bindings and correlation ID.
 *
 * @param asyncApiContext the load the message traits belong to
 */
internal class MessageTraitRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<MessageTrait> {
    private val messageExampleValidator = MessageExampleValidator(asyncApiContext)

    override fun check(
        node: MessageTrait,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        node.headers?.let { walk.schema(it, path.child("Headers")) }
        validateContentType(node, path, walk)
        validateExamples(node, path, walk)
        walk.tags(node.tags, path)
        walk.externalDocs(node.externalDocs, path)
        walk.bindings(node.bindings, path)
        node.correlationId?.let { walk.member(it, CORRELATION_ID, path) }
    }

    private fun validateContentType(
        node: MessageTrait,
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
        node: MessageTrait,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        node.examples?.let { examples ->
            messageExampleValidator.validate(
                examples = examples,
                headersSchema = node.headers,
                payloadSchema = null,
                path = path,
                results = walk.results,
            )
        }
    }
}
