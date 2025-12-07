package dev.banking.asyncapi.generator.core.parser.references

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext

class ReferenceParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseList(parserNode: ParserNode): List<Reference> = buildList {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            add(parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): Reference {
        val reference = parserNode.mandatory($$"$ref").coerce<String>()
        return Reference(
            ref = reference
        ).also { asyncApiContext.register(it, parserNode) }
    }
}
