package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.MESSAGE as MESSAGE_BINDING
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.MESSAGE as MESSAGE_OBJECT

/**
 * Parses AsyncAPI message objects from parser nodes.
 */
internal class MessageParser(
    private val asyncApiContext: AsyncApiContext,
) {

    private val schemaParser = SchemaParser(asyncApiContext)
    private val tagParser = TagParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val messageTraitParser = MessageTraitParser(asyncApiContext)
    private val messageExampleParser = MessageExampleParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val correlationIdParser = CorrelationIdParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, MessageInterface> =
        parserNode.expectObject().members().associate { node ->
            node.name to parseElement(node)
        }

    fun parseElement(parserNode: ParserNode): MessageInterface {
        val objectNode = parserNode.expectObject()
        val reference = objectNode.optional($$"$ref")?.expect<String>()
        return if (reference != null) {
            MessageInterface.MessageReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = MESSAGE,
                ).also { asyncApiContext.register(it, parserNode) },
            )
        } else {
            objectNode.expectOnlyMembers(MESSAGE_OBJECT)
            MessageInterface.MessageInline(
                Message(
                    name = objectNode.optional("name")?.expect<String>(),
                    title = objectNode.optional("title")?.expect<String>(),
                    summary = objectNode.optional("summary")?.expect<String>(),
                    description = objectNode.optional("description")?.expect<String>(),
                    contentType = objectNode.optional("contentType")?.expect<String>(),
                    headers = objectNode.optional("headers")?.let(schemaParser::parseElement),
                    payload = objectNode.optional("payload")?.let(schemaParser::parseElement),
                    correlationId = objectNode.optional("correlationId")?.let(correlationIdParser::parseElement),
                    tags = objectNode.optional("tags")?.let(tagParser::parseList),
                    externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                    bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, MESSAGE_BINDING) },
                    examples = objectNode.optional("examples")?.let(messageExampleParser::parseList),
                    traits = objectNode.optional("traits")?.let(messageTraitParser::parseList),
                ).also { asyncApiContext.register(it, parserNode) },
            )
        }
    }

}
