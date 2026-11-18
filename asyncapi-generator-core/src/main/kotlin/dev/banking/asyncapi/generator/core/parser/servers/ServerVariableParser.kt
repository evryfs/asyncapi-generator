package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER_VARIABLE
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.SERVER_VARIABLE as SERVER_VARIABLE_OBJECT

/**
 * Parses AsyncAPI server variable objects from parser nodes.
 */
internal class ServerVariableParser {
    fun parseElement(parserNode: ParserNode): ServerVariableInterface =
        parserNode.parseReferenceable(
            category = SERVER_VARIABLE,
            objectType = SERVER_VARIABLE_OBJECT,
            reference = ServerVariableInterface::ServerVariableReference,
            inline = ServerVariableInterface::ServerVariableInline,
        ) { objectNode ->
            ServerVariable(
                enum = objectNode.optional("enum")?.expect<List<String>>(),
                default = objectNode.optional("default")?.expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                examples = objectNode.optional("examples")?.expect<List<String>>(),
                extensions = objectNode.extensions(),
            )
        }
}
