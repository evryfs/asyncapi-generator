package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseList
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.MESSAGE as MESSAGE_BINDING
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.MESSAGE as MESSAGE_OBJECT

/**
 * Parses AsyncAPI message objects from parser nodes.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class MessageParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val schemaParser = SchemaParser(asyncApiContext)
    private val tagParser = TagParser()
    private val bindingParser = BindingParser(asyncApiContext)
    private val messageTraitParser = MessageTraitParser(asyncApiContext)
    private val messageExampleParser = MessageExampleParser()
    private val externalDocsParser = ExternalDocsParser()
    private val correlationIdParser = CorrelationIdParser()

    fun parseElement(parserNode: ParserNode): MessageInterface =
        parserNode.parseReferenceable(
            category = MESSAGE,
            objectType = MESSAGE_OBJECT,
            reference = MessageInterface::MessageReference,
            inline = MessageInterface::MessageInline,
        ) { objectNode ->
            Message(
                name = objectNode.optional("name")?.expect<String>(),
                title = objectNode.optional("title")?.expect<String>(),
                summary = objectNode.optional("summary")?.expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                contentType = objectNode.optional("contentType")?.expect<String>(),
                headers = objectNode.optional("headers")?.let(schemaParser::parseElement),
                payload = objectNode.optional("payload")?.let(schemaParser::parseElement),
                correlationId = objectNode.optional("correlationId")?.let(correlationIdParser::parseElement),
                tags = objectNode.optional("tags")?.parseList(tagParser::parseElement),
                externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, MESSAGE_BINDING) },
                examples = objectNode.optional("examples")?.parseList(messageExampleParser::parseElement),
                traits = objectNode.optional("traits")?.parseList(messageTraitParser::parseElement),
                extensions = objectNode.extensions(),
            )
        }
}
