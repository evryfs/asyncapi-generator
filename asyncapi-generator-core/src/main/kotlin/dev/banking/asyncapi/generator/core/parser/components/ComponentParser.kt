package dev.banking.asyncapi.generator.core.parser.components

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
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

    fun parse(node: ParserNode): Component =
        node.parseObject(COMPONENTS) { objectNode ->
            Component(
                schemas = objectNode.optional("schemas")?.parseMap(schemaParser::parseElement),
                servers = objectNode.optional("servers")?.parseMap(serverParser::parseElement),
                channels = objectNode.optional("channels")?.parseMap(channelParser::parseElement),
                operations = objectNode.optional("operations")?.parseMap(operationParser::parseElement),
                messages = objectNode.optional("messages")?.parseMap(messageParser::parseElement),
                securitySchemes = objectNode.optional("securitySchemes")?.parseMap(securitySchemeParser::parseElement),
                serverVariables = objectNode.optional("serverVariables")?.parseMap(serverVariableParser::parseElement),
                parameters = objectNode.optional("parameters")?.parseMap(parameterParser::parseElement),
                correlationIds = objectNode.optional("correlationIds")?.parseMap(correlationIdParser::parseElement),
                replies = objectNode.optional("replies")?.parseMap(operationReplyParser::parseElement),
                replyAddresses =
                    objectNode
                        .optional(
                            "replyAddresses",
                        )?.parseMap(operationReplyAddressParser::parseElement),
                externalDocs = objectNode.optional("externalDocs")?.parseMap(externalDocsParser::parseElement),
                tags = objectNode.optional("tags")?.parseMap(tagParser::parseElement),
                operationTraits = objectNode.optional("operationTraits")?.parseMap(operationTraitParser::parseElement),
                messageTraits = objectNode.optional("messageTraits")?.parseMap(messageTraitParser::parseElement),
                serverBindings =
                    objectNode
                        .optional(
                            "serverBindings",
                        )?.let { bindingParser.parseComponentMap(it, SERVER) },
                channelBindings =
                    objectNode
                        .optional(
                            "channelBindings",
                        )?.let { bindingParser.parseComponentMap(it, CHANNEL) },
                operationBindings =
                    objectNode.optional("operationBindings")?.let {
                        bindingParser.parseComponentMap(it, OPERATION)
                    },
                messageBindings =
                    objectNode
                        .optional(
                            "messageBindings",
                        )?.let { bindingParser.parseComponentMap(it, MESSAGE) },
                extensions = objectNode.extensions(),
            )
        }
}
