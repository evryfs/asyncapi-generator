package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.source.DocumentArray
import dev.banking.asyncapi.generator.core.source.DocumentNode
import dev.banking.asyncapi.generator.core.source.DocumentObject
import dev.banking.asyncapi.generator.core.source.InputDocument
import dev.banking.asyncapi.generator.core.source.SourceId
import dev.banking.asyncapi.generator.core.source.SourceLocation

/**
 * Creates parser nodes from reader-stage input documents.
 *
 * This is the adapter between the reader stage and parser stage. It preserves
 * the existing parser path convention while registering reader-provided source
 * locations in the parser context.
 */
internal object ParserNodeFactory {
    /** Creates the root [ParserNode] from a reader-produced [InputDocument]. */
    fun root(
        document: InputDocument,
        context: AsyncApiContext,
    ): ParserNode {
        val sourceId =
            context.registerDocumentSource(
                file = document.source.file,
                content = document.source.content,
                location = document.root.location,
            )

        val rootAddress = NodeAddress.root(SourceId(sourceId))
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
        context.sourceTracking.registerLocation(address, location)

        when (node) {
            is DocumentObject ->
                node.members.forEach { (name, member) ->
                    registerLocations(
                        node = member.value,
                        address = address.member(name),
                        location = member.keyLocation,
                        context = context,
                    )
                }

            is DocumentArray ->
                node.elements.forEachIndexed { index, element ->
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
