package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext

class ServerVariableParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, ServerVariableInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            val reference = node.optional($$"$ref")?.coerce<String>()
            val serverVariable = if (reference != null) {
                ServerVariableInterface.ServerVariableReference(
                    Reference(
                        ref = reference
                    ).also { asyncApiContext.register(it, node) }
                )
            } else {
                ServerVariableInterface.ServerVariableInline(
                    ServerVariable(
                        enum = node.optional("enum")?.coerce<List<String>>(),
                        default = node.optional("default")?.coerce<String>(),
                        description = node.optional("description")?.coerce<String>(),
                        examples = node.optional("examples")?.coerce<List<String>>(),
                    ).also { asyncApiContext.register(it, node) }
                )
            }
            put(node.name, serverVariable)
        }
    }
}
