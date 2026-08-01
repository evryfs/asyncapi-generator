package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser

/**
 * Parses AsyncAPI server objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `ServerParserTest`
 */
class ServerParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagParser = TagParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val serverVariableParser = ServerVariableParser(asyncApiContext)
    private val securitySchemeParser = SecuritySchemeParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, ServerInterface> = buildMap {
        parserNode.members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): ServerInterface {
        val reference = parserNode.optional($$"$ref")?.expect<String>()
        return if (reference != null) {
            ServerInterface.ServerReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = SERVER,
                ).also { asyncApiContext.register(it, parserNode) },
            )
        } else {
            ServerInterface.ServerInline(
                Server(
                    host = parserNode.required("host").expect<String>(),
                    protocol = parserNode.required("protocol").expect<String>(),
                    protocolVersion = parserNode.optional("protocolVersion")?.expect<String>(),
                    description = parserNode.optional("description")?.expect<String>(),
                    title = parserNode.optional("title")?.expect<String>(),
                    summary = parserNode.optional("summary")?.expect<String>(),
                    variables = parserNode.optional("variables")?.let(serverVariableParser::parseMap),
                    security = parserNode.optional("security")?.let(securitySchemeParser::parseList),
                    bindings = parserNode.optional("bindings")?.let(bindingParser::parseMap),
                    tags = parserNode.optional("tags")?.let(tagParser::parseList),
                    externalDocs = parserNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                ).also { asyncApiContext.register(it, parserNode) },
            )
        }
    }
}
