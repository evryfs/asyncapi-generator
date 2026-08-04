package dev.banking.asyncapi.generator.core.parser.references

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey

/**
 * Parses generic AsyncAPI reference objects from parser nodes.
 */
internal class ReferenceParser(
    private val asyncApiContext: AsyncApiContext,
) {

    fun parseList(
        parserNode: ParserNode,
        category: ReferenceCategoryKey,
    ): List<Reference> =
        parserNode.expectArray().elements().map { node ->
            parseElement(node, category)
        }

    fun parseElement(
        parserNode: ParserNode,
        category: ReferenceCategoryKey,
    ): Reference {
        val reference = parserNode.expectObject().required($$"$ref").expect<String>()
        return Reference(
            ref = reference,
            referenceCategoryKey = category,
        ).also { asyncApiContext.register(it, parserNode) }
    }
}
