package dev.banking.asyncapi.generator.core.parser.correlations

import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CORRELATION_ID

/**
 * Parses AsyncAPI correlation ID objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `CorrelationIdParserTest`
 */
class CorrelationIdParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, CorrelationIdInterface> = buildMap {
        val nodes = parserNode.members()
        nodes.forEach { node ->
            val correlationId = parseElement(node)
            put(node.name, correlationId)
        }
    }

    fun parseElement(node: ParserNode): CorrelationIdInterface {
        node.optional($$"$ref")?.expect<String>()?.let { reference ->
            return CorrelationIdInterface.CorrelationIdReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = CORRELATION_ID
                ).also { asyncApiContext.register(it, node) }
            )
        }
        return CorrelationIdInterface.CorrelationIdInline(
            CorrelationId(
                location = node.required("location").expect<String>(),
                description = node.optional("description")?.expect<String>(),
            ).also { asyncApiContext.register(it, node) }
        )
    }
}
