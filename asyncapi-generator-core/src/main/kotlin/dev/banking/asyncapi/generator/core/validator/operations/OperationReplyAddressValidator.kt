package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY_ADDRESS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REPLY_ADDRESS_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REPLY_ADDRESS_REQUIRED
import dev.banking.asyncapi.generator.core.validator.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.util.requireNonBlank

internal class OperationReplyAddressValidator(
    val asyncApiContext: AsyncApiContext,
) {
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(
        node: OperationReplyAddressInterface,
        contextString: String,
        results: ValidationCollector,
    ) {
        when (node) {
            is OperationReplyAddressInterface.OperationReplyAddressInline ->
                validate(node.operationReplyAddress, contextString, results)

            is OperationReplyAddressInterface.OperationReplyAddressReference ->
                referenceResolver.resolve(node.reference, OPERATION_REPLY_ADDRESS, contextString, results)
        }
    }

    fun validate(
        node: OperationReplyAddress,
        contextString: String,
        results: ValidationCollector,
    ) {
        if (!results.visit(node)) return
        validateLocation(node, contextString, results)
    }

    private fun validateLocation(
        node: OperationReplyAddress,
        contextString: String,
        results: ValidationCollector,
    ) {
        val location = node.location
        val locationSource = asyncApiContext.modelTracking.getSourceLocation(node, node::location)
        if (!results.requireNonBlank(
                location,
                "location",
                contextString,
                REPLY_ADDRESS_REQUIRED,
                locationSource,
            )
        ) {
            return
        }
        if (!ValidationFormats.isRuntimeExpression(location)) {
            results.error(
                REPLY_ADDRESS_FORMAT,
                "$contextString 'location' ('$location') must be a valid runtime expression.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::location),
            )
        }
    }
}
