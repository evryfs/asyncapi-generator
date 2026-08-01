package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE

/**
 * Parses AsyncAPI message objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `MessageParserTest`
 */
class MessageParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val schemaParser = SchemaParser(asyncApiContext)
    private val tagParser = TagParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val messageTraitParser = MessageTraitParser(asyncApiContext)
    private val messageExampleParser = MessageExampleParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val correlationIdParser = CorrelationIdParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, MessageInterface> = buildMap {
        parserNode.members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): MessageInterface {
        parserNode.expectOnlyMembers(
            objectType = "Message Object",
            allowedMembers = MESSAGE_OBJECT_MEMBERS,
        )
        val reference = parserNode.optional($$"$ref")?.expect<String>()
        return if (reference != null) {
            MessageInterface.MessageReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = MESSAGE,
                ).also { asyncApiContext.register(it, parserNode) },
            )
        } else {
            MessageInterface.MessageInline(
                Message(
                    name = parserNode.optional("name")?.expect<String>(),
                    title = parserNode.optional("title")?.expect<String>(),
                    summary = parserNode.optional("summary")?.expect<String>(),
                    description = parserNode.optional("description")?.expect<String>(),
                    contentType = parserNode.optional("contentType")?.expect<String>(),
                    headers = parserNode.optional("headers")?.let(schemaParser::parseElement),
                    payload = parserNode.optional("payload")?.let(schemaParser::parseElement),
                    correlationId = parserNode.optional("correlationId")?.let(correlationIdParser::parseElement),
                    tags = parserNode.optional("tags")?.let(tagParser::parseList),
                    externalDocs = parserNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                    bindings = parserNode.optional("bindings")?.let(bindingParser::parseMap),
                    examples = parserNode.optional("examples")?.let(messageExampleParser::parseList),
                    traits = parserNode.optional("traits")?.let(messageTraitParser::parseList),
                ).also { asyncApiContext.register(it, parserNode) },
            )
        }
    }

    private companion object {
        val MESSAGE_OBJECT_MEMBERS =
            setOf(
                $$"$ref",
                "headers",
                "payload",
                "correlationId",
                "contentType",
                "name",
                "title",
                "summary",
                "description",
                "tags",
                "externalDocs",
                "bindings",
                "examples",
                "traits",
            )
    }
}
