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
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): ServerVariableInterface {
        val reference = parserNode.optional($$"$ref")?.expect<String>()
        return if (reference != null) {
            ServerVariableInterface.ServerVariableReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = SERVER_VARIABLE,
                ).also { asyncApiContext.register(it, parserNode) },
            )
        } else {
            ServerVariableInterface.ServerVariableInline(
                ServerVariable(
                    enum = parserNode.optional("enum")?.expect<List<String>>(),
                    default = parserNode.optional("default")?.expect<String>(),
                    description = parserNode.optional("description")?.expect<String>(),
                    examples = parserNode.optional("examples")?.expect<List<String>>(),
                ).also { asyncApiContext.register(it, parserNode) },
            )
        }
    }
}
