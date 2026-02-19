package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.RUNTIME_EXPRESSION_GENERAL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class OperationReplyAddressValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: OperationReplyAddressInterface, contextString: String, results: ValidationResults) {
        when (node) {
            is OperationReplyAddressInterface.OperationReplyAddressInline ->
                validate(node.operationReplyAddress, contextString, results)

            is OperationReplyAddressInterface.OperationReplyAddressReference ->
                referenceResolver.resolve(node.reference, contextString, results)
        }
    }

    fun validate(node: OperationReplyAddress, contextString: String, results: ValidationResults) {
        validateLocation(node, contextString, results)
    }

    private fun validateLocation(node: OperationReplyAddress, contextString: String, results: ValidationResults) {
        val location = node.location.let(::sanitizeString)
        if (location.isBlank()) {
            results.error(
                "$contextString 'location' is required and cannot be empty.",
                asyncApiContext.getLine(node, node::location),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationReplyAddressObject"
            )
            return
        }
        if (!RUNTIME_EXPRESSION_GENERAL.matches(location)) {
            results.warn(
                "$contextString 'location' ('$location') does not appear to follow a valid runtime expression format.",
                asyncApiContext.getLine(node, node::location)
            )
        }
    }
}
