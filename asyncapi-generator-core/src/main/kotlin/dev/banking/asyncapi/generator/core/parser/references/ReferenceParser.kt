package dev.banking.asyncapi.generator.core.parser.references

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.parser.channels.ChannelParser
import dev.banking.asyncapi.generator.core.parser.messages.MessageParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseList
import dev.banking.asyncapi.generator.core.parser.servers.ServerParser

/**
 * Parses Reference Objects: members that must be a `$ref`, such as an operation's channel.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class ReferenceParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val channelParser by lazy { ChannelParser(asyncApiContext) }
    private val messageParser by lazy { MessageParser(asyncApiContext) }
    private val serverParser by lazy { ServerParser(asyncApiContext) }

    /** Parses a `$ref` to a channel. */
    fun parseChannel(parserNode: ParserNode): Reference = parseElement(parserNode, CHANNEL, channelParser::parseElement)

    /** Parses an array of `$ref`s to messages. */
    fun parseMessages(parserNode: ParserNode): List<Reference> =
        parserNode.parseList { node -> parseElement(node, MESSAGE, messageParser::parseElement) }

    /** Parses an array of `$ref`s to servers. */
    fun parseServers(parserNode: ParserNode): List<Reference> =
        parserNode.parseList { node -> parseElement(node, SERVER, serverParser::parseElement) }

    private fun parseElement(
        parserNode: ParserNode,
        category: ReferenceCategoryKey,
        parseTarget: (ParserNode) -> Any,
    ): Reference {
        val reference = parserNode.expectObject().required($$"$ref").expect<String>()
        return Reference(
            ref = reference,
            referenceCategoryKey = category,
        ).also { asyncApiContext.registerReference(it, parserNode, parseTarget) }
    }
}
