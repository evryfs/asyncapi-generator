package com.tietoevry.banking.asyncapi.generator.core.parser.messages

import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageInterface
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.messages.Message
import com.tietoevry.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import com.tietoevry.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import com.tietoevry.banking.asyncapi.generator.core.parser.tags.TagParser
import com.tietoevry.banking.asyncapi.generator.core.parser.bindings.BindingParser
import com.tietoevry.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext

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
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            val reference = node.optional($$"$ref")?.coerce<String>()
            val messageInterface = if (reference != null) {
                MessageInterface.MessageReference(
                    Reference(
                        ref = reference,
                    ).also { asyncApiContext.register(it, node) }
                )
            } else {
                MessageInterface.MessageInline(
                    Message(
                        name = node.optional("name")?.coerce<String>(),
                        title = node.optional("title")?.coerce<String>(),
                        summary = node.optional("summary")?.coerce<String>(),
                        description = node.optional("description")?.coerce<String>(),
                        contentType = node.optional("contentType")?.coerce<String>(),
                        headers = node.optional("headers")?.let(schemaParser::parseMap),
                        payload = node.optional("payload")?.let(schemaParser::parseElement),
                        correlationId = node.optional("correlationId")?.let(correlationIdParser::parseElement),
                        tags = node.optional("tags")?.let(tagParser::parseList),
                        externalDocs = node.optional("externalDocs")?.let(externalDocsParser::parseElement),
                        bindings = node.optional("bindings")?.let(bindingParser::parseMap),
                        examples = node.optional("examples")?.let(messageExampleParser::parseList),
                        traits = node.optional("traits")?.let(messageTraitParser::parseList)
                    ).also { asyncApiContext.register(it, node) }
                )
            }
            put(node.name, messageInterface)
        }
    }
}
