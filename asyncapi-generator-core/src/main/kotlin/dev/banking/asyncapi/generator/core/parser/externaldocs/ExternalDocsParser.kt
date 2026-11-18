package dev.banking.asyncapi.generator.core.parser.externaldocs

import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.EXTERNAL_DOCUMENTATION

/**
 * Parses AsyncAPI external documentation objects from parser nodes.
 */
internal class ExternalDocsParser {
    fun parseElement(parserNode: ParserNode): ExternalDocInterface =
        parserNode.parseReferenceable(
            category = EXTERNAL_DOC,
            objectType = EXTERNAL_DOCUMENTATION,
            reference = ExternalDocInterface::ExternalDocReference,
            inline = ExternalDocInterface::ExternalDocInline,
        ) { objectNode ->
            ExternalDoc(
                description = objectNode.optional("description")?.expect<String>(),
                url = objectNode.required("url").expect<String>(),
                extensions = objectNode.extensions(),
            )
        }
}
