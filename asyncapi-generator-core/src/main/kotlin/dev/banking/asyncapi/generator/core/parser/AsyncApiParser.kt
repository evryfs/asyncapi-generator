package dev.banking.asyncapi.generator.core.parser

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.parser.channels.ChannelParser
import dev.banking.asyncapi.generator.core.parser.components.ComponentParser
import dev.banking.asyncapi.generator.core.parser.info.InfoParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseMap
import dev.banking.asyncapi.generator.core.parser.node.parseObject
import dev.banking.asyncapi.generator.core.parser.operations.OperationParser
import dev.banking.asyncapi.generator.core.parser.servers.ServerParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.ASYNC_API
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiSpecificationLine

/**
 * Parses a reader-backed parser-node tree into an AsyncAPI document model.
 *
 * The parser stage consumes typed [ParserNode] values and maps supported
 * AsyncAPI structures into [AsyncApiDocument]. It does not read files, detect
 * input formats, parse YAML or JSON, validate the model, bundle references, or
 * generate output.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class AsyncApiParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val infoParser = InfoParser()
    private val serverParser = ServerParser(asyncApiContext)
    private val channelParser = ChannelParser(asyncApiContext)
    private val operationParser = OperationParser(asyncApiContext)
    private val componentParser = ComponentParser(asyncApiContext)

    fun parse(parserNode: ParserNode): AsyncApiDocument =
        AsyncApiSpecificationLine.select(parserNode).parseObject(ASYNC_API) { rootObject ->
            AsyncApiDocument(
                asyncapi = rootObject.required("asyncapi").expect<String>(),
                id = rootObject.optional("id")?.expect<String>(),
                info = rootObject.required("info").let(infoParser::parse),
                servers = rootObject.optional("servers")?.parseMap(serverParser::parseElement),
                defaultContentType = rootObject.optional("defaultContentType")?.expect<String>(),
                channels = rootObject.optional("channels")?.parseMap(channelParser::parseElement),
                operations = rootObject.optional("operations")?.parseMap(operationParser::parseElement),
                components = rootObject.optional("components")?.let(componentParser::parse),
                extensions = rootObject.extensions(),
            )
        }
}
