package com.tietoevry.banking.asyncapi.generator.core.parser

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.parser.channels.ChannelParser
import com.tietoevry.banking.asyncapi.generator.core.parser.components.ComponentParser
import com.tietoevry.banking.asyncapi.generator.core.parser.info.InfoParser
import com.tietoevry.banking.asyncapi.generator.core.parser.operations.OperationParser
import com.tietoevry.banking.asyncapi.generator.core.parser.servers.ServerParser

class AsyncApiParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val infoParser = InfoParser(asyncApiContext)
    private val serverParser = ServerParser(asyncApiContext)
    private val channelParser = ChannelParser(asyncApiContext)
    private val operationParser = OperationParser(asyncApiContext)
    private val componentParser = ComponentParser(asyncApiContext)

    fun parse(parserNode: ParserNode): AsyncApiDocument {
        return AsyncApiDocument(
            asyncapi = parserNode.mandatory("asyncapi").coerce<String>(),
            id = parserNode.optional("id")?.coerce<String>(),
            info = parserNode.mandatory("info").let(infoParser::parseMap),
            servers = parserNode.optional("servers")?.let(serverParser::parseMap),
            defaultContentType = parserNode.optional("defaultContentType")?.coerce<String>(),
            channels = parserNode.optional("channels")?.let(channelParser::parseMap),
            operations = parserNode.optional("operations")?.let(operationParser::parseMap),
            components = parserNode.optional("components")?.let(componentParser::parseElement),
        ).also { asyncApiContext.register(it, parserNode) }
    }
}
