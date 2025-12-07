package com.tietoevry.banking.asyncapi.generator.core.parser.components

import com.tietoevry.banking.asyncapi.generator.core.model.components.Component
import com.tietoevry.banking.asyncapi.generator.core.model.components.ComponentInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.channels.ChannelParser
import com.tietoevry.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import com.tietoevry.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import com.tietoevry.banking.asyncapi.generator.core.parser.parameters.ParameterParser
import com.tietoevry.banking.asyncapi.generator.core.parser.tags.TagParser
import com.tietoevry.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import com.tietoevry.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import com.tietoevry.banking.asyncapi.generator.core.parser.messages.MessageParser
import com.tietoevry.banking.asyncapi.generator.core.parser.messages.MessageTraitParser
import com.tietoevry.banking.asyncapi.generator.core.parser.operations.OperationParser
import com.tietoevry.banking.asyncapi.generator.core.parser.operations.OperationReplyAddressParser
import com.tietoevry.banking.asyncapi.generator.core.parser.operations.OperationReplyParser
import com.tietoevry.banking.asyncapi.generator.core.parser.operations.OperationTraitParser
import com.tietoevry.banking.asyncapi.generator.core.parser.servers.ServerParser
import com.tietoevry.banking.asyncapi.generator.core.parser.servers.ServerVariableParser
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.parser.bindings.BindingParser

class ComponentParser(
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

    fun parseElement(node: ParserNode): ComponentInterface =
        ComponentInterface.ComponentInline(
            Component(
                schemas = node.optional("schemas")?.let(schemaParser::parseMap),
                servers = node.optional("servers")?.let(serverParser::parseMap),
                channels = node.optional("channels")?.let(channelParser::parseMap),
                operations = node.optional("operations")?.let(operationParser::parseMap),
                messages = node.optional("messages")?.let(messageParser::parseMap),
                securitySchemes = node.optional("securitySchemes")?.let(securitySchemeParser::parseMap),
                serverVariables = node.optional("serverVariables")?.let(serverVariableParser::parseMap),
                parameters = node.optional("parameters")?.let(parameterParser::parseMap),
                correlationIds = node.optional("correlationIds")?.let(correlationIdParser::parseMap),
                replies = node.optional("replies")?.let(operationReplyParser::parseMap),
                replyAddresses = node.optional("replyAddresses")?.let(operationReplyAddressParser::parseMap),
                externalDocs = node.optional("externalDocs")?.let(externalDocsParser::parseMap),
                tags = node.optional("tags")?.let(tagParser::parseMap),
                operationTraits = node.optional("operationTraits")?.let(operationTraitParser::parseMap),
                messageTraits = node.optional("messageTraits")?.let(messageTraitParser::parseMap),
                serverBindings = node.optional("serverBindings")?.let(bindingParser::parseMap),
                channelBindings = node.optional("channelBindings")?.let(bindingParser::parseMap),
                operationBindings = node.optional("operationBindings")?.let(bindingParser::parseMap),
                messageBindings = node.optional("messageBindings")?.let(bindingParser::parseMap),
            ).also { asyncApiContext.register(it, node) }
        )
}
