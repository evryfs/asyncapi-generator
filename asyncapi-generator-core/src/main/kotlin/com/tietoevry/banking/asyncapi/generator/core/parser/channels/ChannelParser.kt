package com.tietoevry.banking.asyncapi.generator.core.parser.channels

import com.tietoevry.banking.asyncapi.generator.core.model.channels.Channel
import com.tietoevry.banking.asyncapi.generator.core.model.channels.ChannelInterface
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import com.tietoevry.banking.asyncapi.generator.core.parser.parameters.ParameterParser
import com.tietoevry.banking.asyncapi.generator.core.parser.tags.TagParser
import com.tietoevry.banking.asyncapi.generator.core.parser.bindings.BindingParser
import com.tietoevry.banking.asyncapi.generator.core.parser.messages.MessageParser
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.parser.references.ReferenceParser

class ChannelParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagParser: TagParser = TagParser(asyncApiContext)
    private val referenceParser = ReferenceParser(asyncApiContext)
    private val messageParser: MessageParser = MessageParser(asyncApiContext)
    private val bindingParser: BindingParser = BindingParser(asyncApiContext)
    private val parameterParser: ParameterParser = ParameterParser(asyncApiContext)
    private val externalDocsParser: ExternalDocsParser = ExternalDocsParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, ChannelInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            val reference = node.optional($$"$ref")?.coerce<String>()
            val channel = if (reference != null) {
                ChannelInterface.ChannelReference(
                    Reference(
                        ref = reference
                    ).also { asyncApiContext.register(it, node) }
                )
            } else {
                ChannelInterface.ChannelInline(
                    Channel(
                        address = node.optional("address")?.coerce<String>(),
                        messages = node.optional("messages")?.let(messageParser::parseMap),
                        title = node.optional("title")?.coerce<String>(),
                        summary = node.optional("summary")?.coerce<String>(),
                        description = node.optional("description")?.coerce<String>(),
                        servers = node.optional("servers")?.let(referenceParser::parseList),
                        parameters = node.optional("parameters")?.let(parameterParser::parseMap),
                        tags = node.optional("tags")?.let(tagParser::parseList),
                        externalDocs = node.optional("externalDocs")?.let(externalDocsParser::parseElement),
                        bindings = node.optional("bindings")?.let(bindingParser::parseMap),
                    ).also { asyncApiContext.register(it, node) }
                )
            }
            put(node.name, channel)
        }
    }
}
