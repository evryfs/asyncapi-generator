package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentNode
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.InputDocument
import dev.banking.asyncapi.generator.core.document.SourceLocation

/**
 * Creates parser nodes from reader-stage input documents.
 *
 * This is the adapter between the reader stage and parser stage. It preserves
 * the existing parser path convention while registering reader-provided source
 * locations in the parser context.
 *
 * Expected behavior is covered by:
 * - `ParserNodeFactoryTest`
 */
internal object ParserNodeFactory {

    fun root(
        document: InputDocument,
        context: AsyncApiContext,
    ): ParserNode {
        val sourceId = context.registerDocumentSource(
            file = document.source.file,
            content = document.source.content,
            location = document.root.location,
        )

        val rootAddress = NodeAddress.root(sourceId)
        registerLocations(
            node = document.root,
            address = rootAddress,
            location = document.root.location,
            context = context,
        )

        return ParserNode(
            name = rootAddress.displayPath,
            node = document.root,
            address = rootAddress,
            context = context,
        )
    }

    private fun registerLocations(
        node: DocumentNode,
        address: NodeAddress,
        location: SourceLocation,
        context: AsyncApiContext,
    ) {
        context.registerSourceLocation(address, location)

        when (node) {
            is DocumentObject -> node.members.forEach { (name, member) ->
                registerLocations(
                    node = member.value,
                    address = address.member(name),
                    location = member.keyLocation,
                    context = context,
                )
            }

            is DocumentArray -> node.elements.forEachIndexed { index, element ->
                registerLocations(
                    node = element,
                    address = address.index(index),
                    location = element.location,
                    context = context,
                )
            }

            else -> Unit
        }
    }
}
