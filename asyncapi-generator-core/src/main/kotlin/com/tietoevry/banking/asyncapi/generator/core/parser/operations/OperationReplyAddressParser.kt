package com.tietoevry.banking.asyncapi.generator.core.parser.operations

import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext

class OperationReplyAddressParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, OperationReplyAddressInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): OperationReplyAddressInterface {
        parserNode.optional($$"$ref")?.coerce<String>()?.let { reference ->
            return OperationReplyAddressInterface.OperationReplyAddressReference(
                Reference(
                    ref = reference,
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        return OperationReplyAddressInterface.OperationReplyAddressInline(
            OperationReplyAddress(
                location = parserNode.mandatory("location").coerce<String>(),
                description = parserNode.optional("description")?.coerce<String>()
            ).also { asyncApiContext.register(it, parserNode) }
        )
    }
}
