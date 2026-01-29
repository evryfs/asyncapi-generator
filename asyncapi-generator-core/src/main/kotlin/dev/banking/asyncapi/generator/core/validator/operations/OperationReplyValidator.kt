package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults

class OperationReplyValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val operationReplyAddressValidator = OperationReplyAddressValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: OperationReplyInterface, contextString: String, results: ValidationResults) {
        when (node) {
            is OperationReplyInterface.OperationReplyInline ->
                validate(node.operationReply, contextString, results)

            is OperationReplyInterface.OperationReplyReference ->
                referenceResolver.resolve(node.reference, contextString, results)
        }
    }

    fun validate(node: OperationReply, contextString: String, results: ValidationResults) {
        validateAddress(node, contextString, results)
        validateChannel(node, contextString, results)
        validateMessages(node, contextString, results)
    }

    private fun validateAddress(node: OperationReply, contextString: String, results: ValidationResults) {
        val address = node.address ?: return
        val contextString = "$contextString Operation Reply Address"
        when (address) {
            is OperationReplyAddressInterface.OperationReplyAddressInline ->
                operationReplyAddressValidator.validate(address.operationReplyAddress, contextString, results)

            is OperationReplyAddressInterface.OperationReplyAddressReference ->
                referenceResolver.resolve(address.reference, contextString, results)
        }
    }

    private fun validateChannel(node: OperationReply, contextString: String, results: ValidationResults) {
        val channelRef = node.channel ?: return
        val contextString = "$contextString Channel"
        referenceResolver.resolve(channelRef, contextString, results)
    }

    private fun validateMessages(node: OperationReply, operationReplyName: String, results: ValidationResults) {
        val messages = node.messages ?: return
        if (messages.isEmpty()) {
            results.warn(
                "$operationReplyName 'messages' is an empty list — omit it if unused.",
                asyncApiContext.getLine(node, node::messages)
            )
            return
        }
        messages.forEach { messageReference ->
            val contextString = "$operationReplyName Message"
            referenceResolver.resolve(messageReference, contextString, results)
        }
    }
}
