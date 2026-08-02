package dev.banking.asyncapi.generator.core.parser.channels

import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.parameters.ParameterParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.CHANNEL as CHANNEL_BINDING
import dev.banking.asyncapi.generator.core.parser.messages.MessageParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.parser.references.ReferenceParser

/**
 * Parses AsyncAPI channel objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `ChannelParserTest`
 */
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
        parserNode.expectObject().members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): ChannelInterface {
        val objectNode = parserNode.expectObject()
        val reference = objectNode.optional($$"$ref")?.expect<String>()
        return if (reference != null) {
            ChannelInterface.ChannelReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = CHANNEL,
                ).also { asyncApiContext.register(it, parserNode) },
            )
        } else {
            ChannelInterface.ChannelInline(
                Channel(
                    address = objectNode.optional("address")?.expect<String>(),
                    messages = objectNode.optional("messages")?.let(messageParser::parseMap),
                    title = objectNode.optional("title")?.expect<String>(),
                    summary = objectNode.optional("summary")?.expect<String>(),
                    description = objectNode.optional("description")?.expect<String>(),
                    servers = objectNode.optional("servers")?.let { referenceParser.parseList(it, SERVER) },
                    parameters = objectNode.optional("parameters")?.let(parameterParser::parseMap),
                    tags = objectNode.optional("tags")?.let(tagParser::parseList),
                    externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                    bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, CHANNEL_BINDING) },
                ).also { asyncApiContext.register(it, parserNode) },
            )
        }
    }
}
