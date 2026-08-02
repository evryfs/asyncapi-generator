package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY_ADDRESS

/**
 * Parses AsyncAPI operation reply address objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `OperationReplyAddressParserTest`
 * - `OperationReplyParserTest`
 */
class OperationReplyAddressParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, OperationReplyAddressInterface> = buildMap {
        parserNode.expectObject().members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): OperationReplyAddressInterface {
        val objectNode = parserNode.expectObject()
        objectNode.optional($$"$ref")?.expect<String>()?.let { reference ->
            return OperationReplyAddressInterface.OperationReplyAddressReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = OPERATION_REPLY_ADDRESS
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        return OperationReplyAddressInterface.OperationReplyAddressInline(
            OperationReplyAddress(
                location = objectNode.required("location").expect<String>(),
                description = objectNode.optional("description")?.expect<String>()
            ).also { asyncApiContext.register(it, parserNode) }
        )
    }
}
