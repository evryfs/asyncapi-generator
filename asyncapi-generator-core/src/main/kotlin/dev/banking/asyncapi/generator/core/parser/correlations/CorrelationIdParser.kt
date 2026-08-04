package dev.banking.asyncapi.generator.core.parser.correlations

import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CORRELATION_ID
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.CORRELATION_ID as CORRELATION_ID_OBJECT

/**
 * Parses AsyncAPI correlation ID objects from parser nodes.
 */
internal class CorrelationIdParser(
    private val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, CorrelationIdInterface> =
        parserNode.expectObject().members().associate { node ->
            node.name to parseElement(node)
        }

    fun parseElement(node: ParserNode): CorrelationIdInterface {
        val objectNode = node.expectObject()
        objectNode.optional($$"$ref")?.expect<String>()?.let { reference ->
            return CorrelationIdInterface.CorrelationIdReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = CORRELATION_ID
                ).also { asyncApiContext.register(it, node) }
            )
        }
        objectNode.expectOnlyMembers(CORRELATION_ID_OBJECT)
        return CorrelationIdInterface.CorrelationIdInline(
            CorrelationId(
                location = objectNode.required("location").expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
            ).also { asyncApiContext.register(it, node) }
        )
    }
}
