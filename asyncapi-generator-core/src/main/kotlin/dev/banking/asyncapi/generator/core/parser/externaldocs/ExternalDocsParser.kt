package dev.banking.asyncapi.generator.core.parser.externaldocs

import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.EXTERNAL_DOCUMENTATION

/**
 * Parses AsyncAPI external documentation objects from parser nodes.
 */
internal class ExternalDocsParser(
    private val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, ExternalDocInterface> =
        parserNode.expectObject().members().associate { node ->
            node.name to parseElement(node)
        }

    fun parseElement(node: ParserNode): ExternalDocInterface {
        val objectNode = node.expectObject()
        objectNode.optional($$"$ref")?.expect<String>()?.let { reference ->
            return ExternalDocInterface.ExternalDocReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = EXTERNAL_DOC
                ).also { asyncApiContext.register(it, node) }
            )
        }
        objectNode.expectOnlyMembers(EXTERNAL_DOCUMENTATION)
        return ExternalDocInterface.ExternalDocInline(
            ExternalDoc(
                description = objectNode.optional("description")?.expect<String>(),
                url = objectNode.required("url").expect<String>()
            ).also { asyncApiContext.register(it, node) }
        )
    }
}
