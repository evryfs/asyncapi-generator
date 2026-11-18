package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY_ADDRESS
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.OPERATION_REPLY_ADDRESS as REPLY_ADDRESS_OBJECT

/**
 * Parses AsyncAPI operation reply address objects from parser nodes.
 */
internal class OperationReplyAddressParser {
    fun parseElement(parserNode: ParserNode): OperationReplyAddressInterface =
        parserNode.parseReferenceable(
            category = OPERATION_REPLY_ADDRESS,
            objectType = REPLY_ADDRESS_OBJECT,
            reference = OperationReplyAddressInterface::OperationReplyAddressReference,
            inline = OperationReplyAddressInterface::OperationReplyAddressInline,
        ) { objectNode ->
            OperationReplyAddress(
                location = objectNode.required("location").expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                extensions = objectNode.extensions(),
            )
        }
}
