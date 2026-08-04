package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.document.DocumentArray

/**
 * Array-shaped view of a [ParserNode].
 *
 * This view owns indexed navigation after the source node has been checked by
 * [ParserNode.expectArray].
 */
internal class ParserArrayNode(
    private val parserNode: ParserNode,
    private val documentArray: DocumentArray,
) {

    fun elements(): List<ParserNode> =
        documentArray.elements.mapIndexed { index, element ->
            parserNode.index(
                index = index,
                node = element,
            )
        }
}
