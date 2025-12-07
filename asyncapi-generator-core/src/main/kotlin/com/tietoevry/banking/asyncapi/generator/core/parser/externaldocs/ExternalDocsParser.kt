package com.tietoevry.banking.asyncapi.generator.core.parser.externaldocs

import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext

class ExternalDocsParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, ExternalDocInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            val externalDoc = parseElement(node)
            put(node.name, externalDoc)
        }
    }

    fun parseElement(node: ParserNode): ExternalDocInterface {
        node.optional($$"$ref")?.coerce<String>()?.let { reference ->
            return ExternalDocInterface.ExternalDocReference(
                Reference(
                    ref = reference
                ).also { asyncApiContext.register(it, node) }
            )
        }
        return ExternalDocInterface.ExternalDocInline(
            ExternalDoc(
                description = node.optional("description")?.coerce<String>(),
                url = node.mandatory("url").coerce<String>()
            ).also { asyncApiContext.register(it, node) }
        )
    }
}
