package dev.banking.asyncapi.generator.core.parser.correlations

import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CORRELATION_ID
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.CORRELATION_ID as CORRELATION_ID_OBJECT

/**
 * Parses AsyncAPI correlation ID objects from parser nodes.
 */
internal class CorrelationIdParser {
    fun parseElement(parserNode: ParserNode): CorrelationIdInterface =
        parserNode.parseReferenceable(
            category = CORRELATION_ID,
            objectType = CORRELATION_ID_OBJECT,
            reference = CorrelationIdInterface::CorrelationIdReference,
            inline = CorrelationIdInterface::CorrelationIdInline,
        ) { objectNode ->
            CorrelationId(
                location = objectNode.required("location").expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                extensions = objectNode.extensions(),
            )
        }
}
