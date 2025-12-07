package com.tietoevry.banking.asyncapi.generator.core.validator.operations

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationReply
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.resolver.ReferenceResolver
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidationResults

class OperationReplyValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val replyAddressValidator = OperationReplyAddressValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(operationReplyName: String, node: OperationReplyInterface, results: ValidationResults) {
        when (node) {
            is OperationReplyInterface.OperationReplyInline ->
                validate(node.operationReply, operationReplyName, results)
            is OperationReplyInterface.OperationReplyReference ->
                referenceResolver.resolve(operationReplyName, node.reference, "Operation Reply", results)
        }
    }

    fun validate(node: OperationReply, operationReplyName: String, results: ValidationResults) {
        validateAddress(node, operationReplyName, results)
        validateChannel(node, operationReplyName, results)
        validateMessages(node, operationReplyName, results)
    }

    private fun validateAddress(node: OperationReply, operationReplyName: String, results: ValidationResults) {
        val address = node.address
            ?: return
        when (address) {
            is OperationReplyAddressInterface.OperationReplyAddressInline ->
                replyAddressValidator.validate(address.operationReplyAddress, operationReplyName, results)
            is OperationReplyAddressInterface.OperationReplyAddressReference ->
                referenceResolver.resolve(operationReplyName, address.reference, "Operation Reply Address", results)
        }
    }

    private fun validateChannel(node: OperationReply, operationReplyName: String, results: ValidationResults) {
        val channelRef = node.channel
            ?: return
        referenceResolver.resolve(operationReplyName, channelRef, "Operation Reply Channel", results)
    }

    private fun validateMessages(node: OperationReply, operationReplyName: String, results: ValidationResults) {
        val messages = node.messages
            ?: return
        if (messages.isEmpty()) {
            results.warn(
                "Operation reply '$operationReplyName' defines an empty 'messages' list — omit it if unused.",
                asyncApiContext.getLine(node, node::messages)
            )
            return
        }
        messages.forEach { messageReference ->
            referenceResolver.resolve(operationReplyName, messageReference, "Operation Reply Message", results)
        }
    }
}
