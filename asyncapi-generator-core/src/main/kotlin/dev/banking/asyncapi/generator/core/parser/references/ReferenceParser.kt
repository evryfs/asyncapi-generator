package dev.banking.asyncapi.generator.core.parser.references

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.REFERENCE

/**
 * Parses generic AsyncAPI reference objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `ReferenceParserTest`
 * - `ChannelParserTest`
 * - `OperationParserTest`
 * - `OperationReplyParserTest`
 */
internal class ReferenceParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseList(
        parserNode: ParserNode,
        category: ReferenceCategoryKey = REFERENCE,
    ): List<Reference> = buildList {
        val nodes = parserNode.expectArray().elements()
        nodes.forEach { node ->
            add(parseElement(node, category))
        }
    }

    fun parseElement(
        parserNode: ParserNode,
        category: ReferenceCategoryKey = REFERENCE,
    ): Reference {
        val reference = parserNode.expectObject().required($$"$ref").expect<String>()
        return Reference(
            ref = reference,
            referenceCategoryKey = category,
        ).also { asyncApiContext.register(it, parserNode) }
    }
}
