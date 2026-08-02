package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY_ADDRESS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_MESSAGES_EMPTY
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

class OperationReplyValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val operationReplyAddressValidator = OperationReplyAddressValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: OperationReplyInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is OperationReplyInterface.OperationReplyInline ->
                validate(node.operationReply, contextString, results)

            is OperationReplyInterface.OperationReplyReference ->
                referenceResolver.resolve(node.reference, OPERATION_REPLY, contextString, results)
        }
    }

    fun validate(node: OperationReply, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validateAddress(node, contextString, results)
        validateChannel(node, contextString, results)
        validateMessages(node, contextString, results)
    }

    private fun validateAddress(node: OperationReply, contextString: String, results: ValidationCollector) {
        val address = node.address ?: return
        val contextString = "$contextString Operation Reply Address"
        when (address) {
            is OperationReplyAddressInterface.OperationReplyAddressInline ->
                operationReplyAddressValidator.validate(address.operationReplyAddress, contextString, results)

            is OperationReplyAddressInterface.OperationReplyAddressReference ->
                referenceResolver.resolve(
                    address.reference,
                    OPERATION_REPLY_ADDRESS,
                    contextString,
                    results,
                )
        }
    }

    private fun validateChannel(node: OperationReply, contextString: String, results: ValidationCollector) {
        val channelRef = node.channel ?: return
        val contextString = "$contextString Channel"
        referenceResolver.resolve(channelRef, CHANNEL, contextString, results)
    }

    private fun validateMessages(node: OperationReply, operationReplyName: String, results: ValidationCollector) {
        val messages = node.messages ?: return
        if (messages.isEmpty()) {
            results.warn(
                OPERATION_REPLY_MESSAGES_EMPTY,
                "$operationReplyName 'messages' is an empty list — omit it if unused.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::messages),
            )
            return
        }
        messages.forEach { messageReference ->
            val contextString = "$operationReplyName Message"
            referenceResolver.resolve(messageReference, MESSAGE, contextString, results)
        }
    }
}
