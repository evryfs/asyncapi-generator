package dev.banking.asyncapi.generator.core.context

import com.fasterxml.jackson.core.JsonPointer
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentObject

internal object ExternalReferenceTargetResolver {
    data class Target(
        val node: ParserNode,
        val objectContainer: ParserNode?,
    )

    fun resolve(rootNode: ParserNode, pointer: JsonPointer): Target? {
        var currentNode = rootNode
        var currentPointer = pointer
        var objectContainer: ParserNode? = null

        while (!currentPointer.matches()) {
            val nextNode = when (val documentNode = currentNode.node) {
                is DocumentObject -> {
                    val memberName = currentPointer.matchingProperty
                    val member = documentNode.member(memberName) ?: return null
                    objectContainer = currentNode
                    ParserNode(
                        name = memberName,
                        node = member.value,
                        path = "${currentNode.path}.$memberName",
                        context = currentNode.context,
                        profile = currentNode.profile,
                    )
                }

                is DocumentArray -> {
                    val index = currentPointer.matchingIndex
                    if (index !in documentNode.elements.indices) return null
                    objectContainer = null
                    ParserNode(
                        name = "${currentNode.name}[$index]",
                        node = documentNode[index],
                        path = "${currentNode.path}[$index]",
                        context = currentNode.context,
                        profile = currentNode.profile,
                    )
                }

                else -> return null
            }
            currentNode = nextNode
            currentPointer = currentPointer.tail()
        }

        return Target(currentNode, objectContainer)
    }
}
