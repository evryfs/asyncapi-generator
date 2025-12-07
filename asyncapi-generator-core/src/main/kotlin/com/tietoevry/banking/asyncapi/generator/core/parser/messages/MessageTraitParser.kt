package com.tietoevry.banking.asyncapi.generator.core.parser.messages

import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageTrait
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import com.tietoevry.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import com.tietoevry.banking.asyncapi.generator.core.parser.tags.TagParser
import com.tietoevry.banking.asyncapi.generator.core.parser.bindings.BindingParser
import com.tietoevry.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext

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
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseList(parserNode: ParserNode): List<MessageTraitInterface> = buildList {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            add(parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): MessageTraitInterface {
        parserNode.coerce<Map<String, Any?>>()
        val reference = parserNode.optional($$"$ref")?.coerce<String>()
        val messageTraitInterface = if (reference != null) {
            MessageTraitInterface.ReferenceMessageTrait(
                Reference(
                    ref = reference,
                ).also { asyncApiContext.register(it, parserNode) }
            )
        } else {
            MessageTraitInterface.InlineMessageTrait(
                MessageTrait(
                    headers = parserNode.optional("headers")?.let(schemaParser::parseMap),
                    correlationId = parserNode.optional("correlationId")?.let(correlationIdParser::parseElement),
                    contentType = parserNode.optional("contentType")?.coerce<String>(),
                    name = parserNode.optional("name")?.coerce<String>(),
                    title = parserNode.optional("title")?.coerce<String>(),
                    summary = parserNode.optional("summary")?.coerce<String>(),
                    description = parserNode.optional("description")?.coerce<String>(),
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
