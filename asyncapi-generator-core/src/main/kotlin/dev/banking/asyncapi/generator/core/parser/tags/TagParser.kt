package dev.banking.asyncapi.generator.core.parser.tags

import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.TAG as TAG_OBJECT

/**
 * Parses AsyncAPI tag objects from parser nodes.
 */
internal class TagParser {
    private val externalDocsParser = ExternalDocsParser()

    fun parseElement(parserNode: ParserNode): TagInterface =
        parserNode.parseReferenceable(
            category = TAG,
            objectType = TAG_OBJECT,
            reference = TagInterface::TagReference,
            inline = TagInterface::TagInline,
        ) { objectNode ->
            Tag(
                name = objectNode.required("name").expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                extensions = objectNode.extensions(),
            )
        }
}
