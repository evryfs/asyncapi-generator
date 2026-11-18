package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.references.ReferenceParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.OPERATION_REPLY as OPERATION_REPLY_OBJECT

/**
 * Parses AsyncAPI operation reply objects from parser nodes.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class OperationReplyParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val referenceParser = ReferenceParser(asyncApiContext)
    private val operationReplyAddressParser = OperationReplyAddressParser()

    fun parseElement(parserNode: ParserNode): OperationReplyInterface =
        parserNode.parseReferenceable(
            category = OPERATION_REPLY,
            objectType = OPERATION_REPLY_OBJECT,
            reference = OperationReplyInterface::OperationReplyReference,
            inline = OperationReplyInterface::OperationReplyInline,
        ) { objectNode ->
            OperationReply(
                address = objectNode.optional("address")?.let(operationReplyAddressParser::parseElement),
                channel = objectNode.optional("channel")?.let(referenceParser::parseChannel),
                messages = objectNode.optional("messages")?.let(referenceParser::parseMessages),
                extensions = objectNode.extensions(),
            )
        }
}
