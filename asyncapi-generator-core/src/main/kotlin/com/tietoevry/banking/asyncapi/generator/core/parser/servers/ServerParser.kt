package com.tietoevry.banking.asyncapi.generator.core.parser.servers

import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.servers.Server
import com.tietoevry.banking.asyncapi.generator.core.model.servers.ServerInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import com.tietoevry.banking.asyncapi.generator.core.parser.tags.TagParser
import com.tietoevry.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.parser.bindings.BindingParser

class ServerParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagParser = TagParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val serverVariableParser = ServerVariableParser(asyncApiContext)
    private val securitySchemeParser = SecuritySchemeParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, ServerInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            val reference = node.optional($$"$ref")?.coerce<String>()
            val serverInterface = if (reference != null) {
                ServerInterface.ServerReference(
                    Reference(
                        ref = reference,
                    ).also { asyncApiContext.register(it, node) }
                )
            } else {
                ServerInterface.ServerInline(
                    Server(
                        host = node.mandatory("host").coerce<String>(),
                        protocol = node.mandatory("protocol").coerce<String>(),
                        protocolVersion = node.optional("protocolVersion")?.coerce<String>(),
                        description = node.optional("description")?.coerce<String>(),
                        title = node.optional("title")?.coerce<String>(),
                        summary = node.optional("summary")?.coerce<String>(),
                        variables = node.optional("variables")?.let(serverVariableParser::parseMap),
                        security = node.optional("security")?.let(securitySchemeParser::parseList),
                        bindings = node.optional("bindings")?.let(bindingParser::parseMap),
                        tags = node.optional("tags")?.let(tagParser::parseList),
                        externalDocs = node.optional("externalDocs")?.let(externalDocsParser::parseElement),
                    ).also { asyncApiContext.register(it, node) }
                )
            }
            put(node.name, serverInterface)
        }
    }
}
