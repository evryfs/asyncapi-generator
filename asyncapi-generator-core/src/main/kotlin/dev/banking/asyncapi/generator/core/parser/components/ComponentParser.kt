package dev.banking.asyncapi.generator.core.parser.components

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.CHANNEL
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.MESSAGE
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.OPERATION
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.SERVER
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.channels.ChannelParser
import dev.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.messages.MessageParser
import dev.banking.asyncapi.generator.core.parser.messages.MessageTraitParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.ParserObjectNode
import dev.banking.asyncapi.generator.core.parser.node.parseMap
import dev.banking.asyncapi.generator.core.parser.node.parseObject
import dev.banking.asyncapi.generator.core.parser.operations.OperationParser
import dev.banking.asyncapi.generator.core.parser.operations.OperationReplyAddressParser
import dev.banking.asyncapi.generator.core.parser.operations.OperationReplyParser
import dev.banking.asyncapi.generator.core.parser.operations.OperationTraitParser
import dev.banking.asyncapi.generator.core.parser.parameters.ParameterParser
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import dev.banking.asyncapi.generator.core.parser.servers.ServerParser
import dev.banking.asyncapi.generator.core.parser.servers.ServerVariableParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.COMPONENTS

/**
 * Parses AsyncAPI component objects from parser nodes.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class ComponentParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val tagParser = TagParser()
    private val schemaParser = SchemaParser(asyncApiContext)
    private val serverParser = ServerParser(asyncApiContext)
    private val channelParser = ChannelParser(asyncApiContext)
    private val messageParser = MessageParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val operationParser = OperationParser(asyncApiContext)
    private val parameterParser = ParameterParser()
    private val externalDocsParser = ExternalDocsParser()
    private val messageTraitParser = MessageTraitParser(asyncApiContext)
    private val correlationIdParser = CorrelationIdParser()
    private val securitySchemeParser = SecuritySchemeParser()
    private val serverVariableParser = ServerVariableParser()
    private val operationReplyParser = OperationReplyParser(asyncApiContext)
    private val operationTraitParser = OperationTraitParser(asyncApiContext)
    private val operationReplyAddressParser = OperationReplyAddressParser()

    /** Parses the Components Object. */
    fun parse(node: ParserNode): Component =
        node.parseObject(COMPONENTS) { objectNode ->
            Component(
                schemas = objectNode.map("schemas", schemaParser::parseElement),
                servers = objectNode.map("servers", serverParser::parseElement),
                channels = objectNode.map("channels", channelParser::parseElement),
                operations = objectNode.map("operations", operationParser::parseElement),
                messages = objectNode.map("messages", messageParser::parseElement),
                securitySchemes = objectNode.map("securitySchemes", securitySchemeParser::parseElement),
                serverVariables = objectNode.map("serverVariables", serverVariableParser::parseElement),
                parameters = objectNode.map("parameters", parameterParser::parseElement),
                correlationIds = objectNode.map("correlationIds", correlationIdParser::parseElement),
                replies = objectNode.map("replies", operationReplyParser::parseElement),
                replyAddresses = objectNode.map("replyAddresses", operationReplyAddressParser::parseElement),
                externalDocs = objectNode.map("externalDocs", externalDocsParser::parseElement),
                tags = objectNode.map("tags", tagParser::parseElement),
                operationTraits = objectNode.map("operationTraits", operationTraitParser::parseElement),
                messageTraits = objectNode.map("messageTraits", messageTraitParser::parseElement),
                serverBindings = objectNode.bindings("serverBindings", SERVER),
                channelBindings = objectNode.bindings("channelBindings", CHANNEL),
                operationBindings = objectNode.bindings("operationBindings", OPERATION),
                messageBindings = objectNode.bindings("messageBindings", MESSAGE),
                extensions = objectNode.extensions(),
            )
        }

    private fun <T> ParserObjectNode.map(
        name: String,
        parseMember: (ParserNode) -> T,
    ): Map<String, T>? = optional(name)?.parseMap(parseMember)

    private fun ParserObjectNode.bindings(
        name: String,
        location: BindingLocation,
    ) = optional(name)?.let { bindingParser.parseComponentMap(it, location) }
}
