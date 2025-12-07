package dev.banking.asyncapi.generator.core.parser.correlations

import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.context.AsyncApiContext

class CorrelationIdParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, CorrelationIdInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            val correlationId = parseElement(node)
            put(node.name, correlationId)
        }
    }

    fun parseElement(node: ParserNode): CorrelationIdInterface {
        node.optional($$"$ref")?.coerce<String>()?.let { reference ->
            return CorrelationIdInterface.CorrelationIdReference(
                Reference(
                    ref = reference,
                ).also { asyncApiContext.register(it, node) }
            )
        }
        return CorrelationIdInterface.CorrelationIdInline(
            CorrelationId(
                location = node.mandatory("location").coerce<String>(),
                description = node.optional("description")?.coerce<String>(),
            ).also { asyncApiContext.register(it, node) }
        )
    }
}
