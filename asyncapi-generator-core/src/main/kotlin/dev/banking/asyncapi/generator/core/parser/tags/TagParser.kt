package dev.banking.asyncapi.generator.core.parser.tags

import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser

/**
 * Parses AsyncAPI tag objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `TagParserTest`
 */
class TagParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val externalDocsParser = ExternalDocsParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, TagInterface> = buildMap {
        parserNode.expectObject().members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseList(parserNode: ParserNode): List<TagInterface> = buildList {
        parserNode.expectArray().elements().forEach { node ->
            add(parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): TagInterface {
        val objectNode = parserNode.expectObject()
        val reference = objectNode.optional($$"$ref")?.expect<String>()
        val tagInterface = if (reference != null) {
            TagInterface.TagReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = TAG
                ).also { asyncApiContext.register(it, parserNode) }
            )
        } else {
            TagInterface.TagInline(
                Tag(
                    name = objectNode.required("name").expect<String>(),
                    description = objectNode.optional("description")?.expect<String>(),
                    externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        return tagInterface
    }
}
