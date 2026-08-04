package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.parser.references.ReferenceParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.OPERATION_REPLY as OPERATION_REPLY_OBJECT

/**
 * Parses AsyncAPI operation reply objects from parser nodes.
 */
internal class OperationReplyParser(
    private val asyncApiContext: AsyncApiContext,
) {

    private val referenceParser = ReferenceParser(asyncApiContext)
    private val operationReplyAddressParser = OperationReplyAddressParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, OperationReplyInterface> =
        parserNode.expectObject().members().associate { node ->
            node.name to parseElement(node)
        }

    fun parseElement(parserNode: ParserNode): OperationReplyInterface {
        val objectNode = parserNode.expectObject()
        val reference = objectNode.optional($$"$ref")?.expect<String>()
        val operationReplyInterface = if (reference != null) {
            OperationReplyInterface.OperationReplyReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = OPERATION_REPLY,
                ).also { asyncApiContext.register(it, parserNode) }
            )
        } else {
            objectNode.expectOnlyMembers(OPERATION_REPLY_OBJECT)
            OperationReplyInterface.OperationReplyInline(
                OperationReply(
                    address = objectNode.optional("address")?.let(operationReplyAddressParser::parseElement),
                    channel = objectNode.optional("channel")?.let { referenceParser.parseElement(it, CHANNEL) },
                    messages = objectNode.optional("messages")?.let { referenceParser.parseList(it, MESSAGE) }
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        return operationReplyInterface
    }
}
