package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER_VARIABLE

/**
 * Parses AsyncAPI server variable objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `ServerVariableParserTest`
 * - `ServerParserTest`
 */
class ServerVariableParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, ServerVariableInterface> = buildMap {
        parserNode.members().forEach { node ->
            val reference = node.optional($$"$ref")?.expect<String>()
            val serverVariable = if (reference != null) {
                ServerVariableInterface.ServerVariableReference(
                    Reference(
                        ref = reference,
                        referenceCategoryKey = SERVER_VARIABLE
                    ).also { asyncApiContext.register(it, node) }
                )
            } else {
                ServerVariableInterface.ServerVariableInline(
                    ServerVariable(
                        enum = node.optional("enum")?.expect<List<String>>(),
                        default = node.optional("default")?.expect<String>(),
                        description = node.optional("description")?.expect<String>(),
                        examples = node.optional("examples")?.expect<List<String>>(),
                    ).also { asyncApiContext.register(it, node) }
                )
            }
            put(node.name, serverVariable)
        }
    }
}
