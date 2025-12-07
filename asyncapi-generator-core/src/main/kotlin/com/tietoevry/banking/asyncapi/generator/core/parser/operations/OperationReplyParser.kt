package com.tietoevry.banking.asyncapi.generator.core.parser.operations

import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationReply
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.parser.references.ReferenceParser

class OperationReplyParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val referenceParser = ReferenceParser(asyncApiContext)
    private val operationReplyAddressParser = OperationReplyAddressParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, OperationReplyInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): OperationReplyInterface {
        val reference = parserNode.optional($$"$ref")?.coerce<String>()
        val operationReplyInterface = if (reference != null) {
            OperationReplyInterface.OperationReplyReference(
                Reference(
                    ref = reference,
                ).also { asyncApiContext.register(it, parserNode) }
            )
        } else {
            OperationReplyInterface.OperationReplyInline(
                OperationReply(
                    address = parserNode.optional("address")?.let(operationReplyAddressParser::parseElement),
                    channel = parserNode.optional("channel")?.let(referenceParser::parseElement),
                    messages = parserNode.optional("messages")?.let(referenceParser::parseList)
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        return operationReplyInterface
    }
}
