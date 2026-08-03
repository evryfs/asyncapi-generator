package dev.banking.asyncapi.generator.core.parser.components

import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.parser.channels.ChannelParser
import dev.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.parameters.ParameterParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import dev.banking.asyncapi.generator.core.parser.messages.MessageParser
import dev.banking.asyncapi.generator.core.parser.messages.MessageTraitParser
import dev.banking.asyncapi.generator.core.parser.operations.OperationParser
import dev.banking.asyncapi.generator.core.parser.operations.OperationReplyAddressParser
import dev.banking.asyncapi.generator.core.parser.operations.OperationReplyParser
import dev.banking.asyncapi.generator.core.parser.operations.OperationTraitParser
import dev.banking.asyncapi.generator.core.parser.servers.ServerParser
import dev.banking.asyncapi.generator.core.parser.servers.ServerVariableParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.CHANNEL
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.MESSAGE
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.OPERATION
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.SERVER
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.COMPONENTS

/**
 * Parses AsyncAPI component objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `ComponentParserTest`
 */
internal class ComponentParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagParser = TagParser(asyncApiContext)
    private val schemaParser = SchemaParser(asyncApiContext)
    private val serverParser = ServerParser(asyncApiContext)
    private val channelParser = ChannelParser(asyncApiContext)
    private val messageParser = MessageParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val operationParser = OperationParser(asyncApiContext)
    private val parameterParser = ParameterParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val messageTraitParser = MessageTraitParser(asyncApiContext)
    private val correlationIdParser = CorrelationIdParser(asyncApiContext)
    private val securitySchemeParser = SecuritySchemeParser(asyncApiContext)
    private val serverVariableParser = ServerVariableParser(asyncApiContext)
    private val operationReplyParser = OperationReplyParser(asyncApiContext)
    private val operationTraitParser = OperationTraitParser(asyncApiContext)
    private val operationReplyAddressParser = OperationReplyAddressParser(asyncApiContext)

    fun parseElement(node: ParserNode): ComponentInterface = with(node.expectObject()) {
        expectOnlyMembers(COMPONENTS)
        ComponentInterface.ComponentInline(
            Component(
                schemas = optional("schemas")?.let(schemaParser::parseMap),
                servers = optional("servers")?.let(serverParser::parseMap),
                channels = optional("channels")?.let(channelParser::parseMap),
                operations = optional("operations")?.let(operationParser::parseMap),
                messages = optional("messages")?.let(messageParser::parseMap),
                securitySchemes = optional("securitySchemes")?.let(securitySchemeParser::parseMap),
                serverVariables = optional("serverVariables")?.let(serverVariableParser::parseMap),
                parameters = optional("parameters")?.let(parameterParser::parseMap),
                correlationIds = optional("correlationIds")?.let(correlationIdParser::parseMap),
                replies = optional("replies")?.let(operationReplyParser::parseMap),
                replyAddresses = optional("replyAddresses")?.let(operationReplyAddressParser::parseMap),
                externalDocs = optional("externalDocs")?.let(externalDocsParser::parseMap),
                tags = optional("tags")?.let(tagParser::parseMap),
                operationTraits = optional("operationTraits")?.let(operationTraitParser::parseMap),
                messageTraits = optional("messageTraits")?.let(messageTraitParser::parseMap),
                serverBindings = optional("serverBindings")?.let { bindingParser.parseComponentMap(it, SERVER) },
                channelBindings = optional("channelBindings")?.let { bindingParser.parseComponentMap(it, CHANNEL) },
                operationBindings = optional("operationBindings")?.let {
                    bindingParser.parseComponentMap(it, OPERATION)
                },
                messageBindings = optional("messageBindings")?.let { bindingParser.parseComponentMap(it, MESSAGE) },
            ).also { asyncApiContext.register(it, node) }
        )
    }
}
