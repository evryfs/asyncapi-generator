package dev.banking.asyncapi.generator.core.parser

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.channels.ChannelParser
import dev.banking.asyncapi.generator.core.parser.components.ComponentParser
import dev.banking.asyncapi.generator.core.parser.info.InfoParser
import dev.banking.asyncapi.generator.core.parser.operations.OperationParser
import dev.banking.asyncapi.generator.core.parser.servers.ServerParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiSpecificationLine

/**
 * Parses a reader-backed parser-node tree into an AsyncAPI document model.
 *
 * The parser stage consumes typed [ParserNode] values and maps supported
 * AsyncAPI structures into [AsyncApiDocument]. It does not read files, detect
 * input formats, parse YAML or JSON, validate the model, bundle references, or
 * generate output.
 *
 * Expected behavior is covered by:
 * - `AsyncApiParserTest`
 */
class AsyncApiParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val infoParser = InfoParser(asyncApiContext)
    private val serverParser = ServerParser(asyncApiContext)
    private val channelParser = ChannelParser(asyncApiContext)
    private val operationParser = OperationParser(asyncApiContext)
    private val componentParser = ComponentParser(asyncApiContext)

    fun parse(parserNode: ParserNode): AsyncApiDocument {
        val root = AsyncApiSpecificationLine.select(parserNode)
        return AsyncApiDocument(
            asyncapi = root.required("asyncapi").expect<String>(),
            id = root.optional("id")?.expect<String>(),
            info = root.required("info").let(infoParser::parseMap),
            servers = root.optional("servers")?.let(serverParser::parseMap),
            defaultContentType = root.optional("defaultContentType")?.expect<String>(),
            channels = root.optional("channels")?.let(channelParser::parseMap),
            operations = root.optional("operations")?.let(operationParser::parseMap),
            components = root.optional("components")?.let(componentParser::parseElement),
        ).also { asyncApiContext.register(it, root) }
    }
}
