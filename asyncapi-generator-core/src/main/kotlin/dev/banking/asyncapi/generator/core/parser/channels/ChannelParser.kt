package dev.banking.asyncapi.generator.core.parser.channels

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.messages.MessageParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseList
import dev.banking.asyncapi.generator.core.parser.node.parseMap
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.parameters.ParameterParser
import dev.banking.asyncapi.generator.core.parser.references.ReferenceParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.CHANNEL as CHANNEL_BINDING
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.CHANNEL as CHANNEL_OBJECT

/**
 * Parses AsyncAPI channel objects from parser nodes.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class ChannelParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val tagParser: TagParser = TagParser()
    private val referenceParser = ReferenceParser(asyncApiContext)
    private val messageParser: MessageParser = MessageParser(asyncApiContext)
    private val bindingParser: BindingParser = BindingParser(asyncApiContext)
    private val parameterParser: ParameterParser = ParameterParser()
    private val externalDocsParser: ExternalDocsParser = ExternalDocsParser()

    fun parseElement(parserNode: ParserNode): ChannelInterface =
        parserNode.parseReferenceable(
            category = CHANNEL,
            objectType = CHANNEL_OBJECT,
            reference = ChannelInterface::ChannelReference,
            inline = ChannelInterface::ChannelInline,
        ) { objectNode ->
            Channel(
                address = objectNode.optional("address")?.expect<String?>(),
                messages = objectNode.optional("messages")?.parseMap(messageParser::parseElement),
                title = objectNode.optional("title")?.expect<String>(),
                summary = objectNode.optional("summary")?.expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                servers = objectNode.optional("servers")?.let { referenceParser.parseList(it, SERVER) },
                parameters = objectNode.optional("parameters")?.parseMap(parameterParser::parseElement),
                tags = objectNode.optional("tags")?.parseList(tagParser::parseElement),
                externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, CHANNEL_BINDING) },
                extensions = objectNode.extensions(),
            )
        }
}
