package dev.banking.asyncapi.generator.core.parser.tags

import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.TAG as TAG_OBJECT

/**
 * Parses AsyncAPI tag objects from parser nodes.
 */
internal class TagParser(
    private val asyncApiContext: AsyncApiContext,
) {

    private val externalDocsParser = ExternalDocsParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, TagInterface> =
        parserNode.expectObject().members().associate { node ->
            node.name to parseElement(node)
        }

    fun parseList(parserNode: ParserNode): List<TagInterface> =
        parserNode.expectArray().elements().map(::parseElement)

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
            objectNode.expectOnlyMembers(TAG_OBJECT)
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
