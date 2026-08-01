package dev.banking.asyncapi.generator.core.parser.externaldocs

import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC

/**
 * Parses AsyncAPI external documentation objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `ExternalDocsParserTest`
 */
class ExternalDocsParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, ExternalDocInterface> = buildMap {
        val nodes = parserNode.members()
        nodes.forEach { node ->
            val externalDoc = parseElement(node)
            put(node.name, externalDoc)
        }
    }

    fun parseElement(node: ParserNode): ExternalDocInterface {
        node.optional($$"$ref")?.expect<String>()?.let { reference ->
            return ExternalDocInterface.ExternalDocReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = EXTERNAL_DOC
                ).also { asyncApiContext.register(it, node) }
            )
        }
        return ExternalDocInterface.ExternalDocInline(
            ExternalDoc(
                description = node.optional("description")?.expect<String>(),
                url = node.required("url").expect<String>()
            ).also { asyncApiContext.register(it, node) }
        )
    }
}
