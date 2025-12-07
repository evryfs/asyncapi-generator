package com.tietoevry.banking.asyncapi.generator.core.parser.tags

import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.tags.Tag
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser

class TagParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val externalDocsParser = ExternalDocsParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, TagInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            put(node.name, parseElement(node))
        }
    }

    fun parseList(parserNode: ParserNode): List<TagInterface> = buildList {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            add(parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): TagInterface {
        val reference = parserNode.optional($$"$ref")?.coerce<String>()
        val tagInterface = if (reference != null) {
            TagInterface.TagReference(
                Reference(
                    ref = reference
                ).also { asyncApiContext.register(it, parserNode) }
            )
        } else {
            TagInterface.TagInline(
                Tag(
                    name = parserNode.mandatory("name").coerce<String>(),
                    description = parserNode.optional("description")?.coerce<String>(),
                    externalDocs = parserNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        return tagInterface
    }
}
