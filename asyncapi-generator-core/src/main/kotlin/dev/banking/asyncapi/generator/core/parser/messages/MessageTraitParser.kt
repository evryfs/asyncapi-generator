package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE_TRAIT

/**
 * Parses AsyncAPI message trait objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `MessageTraitParserTest`
 * - `MessageParserTest`
 */
class MessageTraitParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagParser = TagParser(asyncApiContext)
    private val schemaParser = SchemaParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val correlationIdParser = CorrelationIdParser(asyncApiContext)
    private val messageExampleParser = MessageExampleParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, MessageTraitInterface> = buildMap {
        parserNode.members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseList(parserNode: ParserNode): List<MessageTraitInterface> = buildList {
        parserNode.elements().forEach { node ->
            add(parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): MessageTraitInterface {
        val reference = parserNode.optional($$"$ref")?.expect<String>()
        val messageTraitInterface = if (reference != null) {
            MessageTraitInterface.ReferenceMessageTrait(
                Reference(
                    ref = reference,
                    referenceCategoryKey = MESSAGE_TRAIT
                ).also { asyncApiContext.register(it, parserNode) }
            )
        } else {
            MessageTraitInterface.InlineMessageTrait(
                MessageTrait(
                    headers = parserNode.optional("headers")?.let(schemaParser::parseElement),
                    correlationId = parserNode.optional("correlationId")?.let(correlationIdParser::parseElement),
                    contentType = parserNode.optional("contentType")?.expect<String>(),
                    name = parserNode.optional("name")?.expect<String>(),
                    title = parserNode.optional("title")?.expect<String>(),
                    summary = parserNode.optional("summary")?.expect<String>(),
                    description = parserNode.optional("description")?.expect<String>(),
                    tags = parserNode.optional("tags")?.let(tagParser::parseList),
                    externalDocs = parserNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                    bindings = parserNode.optional("bindings")?.let(bindingParser::parseMap),
                    examples = parserNode.optional("examples")?.let(messageExampleParser::parseList),
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        return messageTraitInterface
    }
}
