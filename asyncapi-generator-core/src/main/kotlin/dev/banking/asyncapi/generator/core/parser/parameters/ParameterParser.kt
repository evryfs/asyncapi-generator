package dev.banking.asyncapi.generator.core.parser.parameters

import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.context.AsyncApiContext

class ParameterParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, ParameterInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            val reference = node.optional($$"$ref")?.coerce<String>()
            val parameterInterface = if (reference != null) {
                ParameterInterface.ParameterReference(
                    Reference(
                        ref = reference,
                    ).also { asyncApiContext.register(it, node) }
                )
            } else {
                ParameterInterface.ParameterInline(
                    Parameter(
                        description = node.optional("description")?.coerce<String>(),
                        location = node.optional("location")?.coerce<String>(),
                        enum = node.optional("enum")?.coerce<List<String>>(),
                        default = node.optional("default")?.coerce<String>(),
                        examples = node.optional("examples")?.coerce<List<String>>(),
                    ).also { asyncApiContext.register(it, node) }
                )
            }
            put(node.name, parameterInterface)
        }
    }
}
