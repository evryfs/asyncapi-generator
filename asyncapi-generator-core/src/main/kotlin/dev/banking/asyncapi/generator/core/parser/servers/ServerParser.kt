package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseList
import dev.banking.asyncapi.generator.core.parser.node.parseMap
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.SERVER as SERVER_BINDING
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.SERVER as SERVER_OBJECT

/**
 * Parses AsyncAPI server objects from parser nodes.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class ServerParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val tagParser = TagParser()
    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser()
    private val serverVariableParser = ServerVariableParser()
    private val securitySchemeParser = SecuritySchemeParser()

    fun parseElement(parserNode: ParserNode): ServerInterface =
        parserNode.parseReferenceable(
            category = SERVER,
            objectType = SERVER_OBJECT,
            reference = ServerInterface::ServerReference,
            inline = ServerInterface::ServerInline,
        ) { objectNode ->
            Server(
                host = objectNode.required("host").expect<String>(),
                protocol = objectNode.required("protocol").expect<String>(),
                protocolVersion = objectNode.optional("protocolVersion")?.expect<String>(),
                pathname = objectNode.optional("pathname")?.expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                title = objectNode.optional("title")?.expect<String>(),
                summary = objectNode.optional("summary")?.expect<String>(),
                variables = objectNode.optional("variables")?.parseMap(serverVariableParser::parseElement),
                security = objectNode.optional("security")?.parseList(securitySchemeParser::parseElement),
                bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, SERVER_BINDING) },
                tags = objectNode.optional("tags")?.parseList(tagParser::parseElement),
                externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                extensions = objectNode.extensions(),
            )
        }
}
