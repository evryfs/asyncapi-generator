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
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.SERVER as SERVER_BINDING
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.SERVER as SERVER_OBJECT

/**
 * Parses AsyncAPI server objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `ServerParserTest`
 */
internal class ServerParser(
    private val asyncApiContext: AsyncApiContext,
) {

    private val tagParser = TagParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val serverVariableParser = ServerVariableParser(asyncApiContext)
    private val securitySchemeParser = SecuritySchemeParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, ServerInterface> = buildMap {
        parserNode.expectObject().members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): ServerInterface {
        val objectNode = parserNode.expectObject()
        val reference = objectNode.optional($$"$ref")?.expect<String>()
        return if (reference != null) {
            ServerInterface.ServerReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = SERVER,
                ).also { asyncApiContext.register(it, parserNode) },
            )
        } else {
            objectNode.expectOnlyMembers(SERVER_OBJECT)
            ServerInterface.ServerInline(
                Server(
                    host = objectNode.required("host").expect<String>(),
                    protocol = objectNode.required("protocol").expect<String>(),
                    protocolVersion = objectNode.optional("protocolVersion")?.expect<String>(),
                    description = objectNode.optional("description")?.expect<String>(),
                    title = objectNode.optional("title")?.expect<String>(),
                    summary = objectNode.optional("summary")?.expect<String>(),
                    variables = objectNode.optional("variables")?.let(serverVariableParser::parseMap),
                    security = objectNode.optional("security")?.let(securitySchemeParser::parseList),
                    bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, SERVER_BINDING) },
                    tags = objectNode.optional("tags")?.let(tagParser::parseList),
                    externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                ).also { asyncApiContext.register(it, parserNode) },
            )
        }
    }
}
