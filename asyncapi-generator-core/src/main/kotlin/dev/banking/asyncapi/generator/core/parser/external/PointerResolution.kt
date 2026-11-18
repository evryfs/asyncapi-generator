package dev.banking.asyncapi.generator.core.parser.external

import com.fasterxml.jackson.core.JsonPointer
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.source.DocumentArray
import dev.banking.asyncapi.generator.core.source.DocumentObject

/** The node [pointer] points at below this node, or `null` when it points at nothing. */
internal fun ParserNode.resolvePointer(pointer: JsonPointer): ParserNode? =
    if (pointer.matches()) {
        this
    } else {
        child(pointer)?.resolvePointer(pointer.tail())
    }

private fun ParserNode.child(pointer: JsonPointer): ParserNode? =
    when (val documentNode = node) {
        is DocumentObject ->
            documentNode.member(pointer.matchingProperty)?.let { member ->
                member(pointer.matchingProperty, member.value)
            }

        is DocumentArray ->
            pointer.matchingIndex
                .takeIf { index -> index in documentNode.elements.indices }
                ?.let { index -> index(index, documentNode[index]) }

        else -> null
    }
