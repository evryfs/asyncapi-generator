package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.RUNTIME_EXPRESSION_GENERAL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY_ADDRESS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_ADDRESS_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_ADDRESS_REQUIRED
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class OperationReplyAddressValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: OperationReplyAddressInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is OperationReplyAddressInterface.OperationReplyAddressInline ->
                validate(node.operationReplyAddress, contextString, results)

            is OperationReplyAddressInterface.OperationReplyAddressReference ->
                referenceResolver.resolve(node.reference, OPERATION_REPLY_ADDRESS, contextString, results)
        }
    }

    fun validate(node: OperationReplyAddress, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validateLocation(node, contextString, results)
    }

    private fun validateLocation(node: OperationReplyAddress, contextString: String, results: ValidationCollector) {
        val location = node.location.let(::sanitizeString)
        if (location.isBlank()) {
            results.error(
                OPERATION_REPLY_ADDRESS_REQUIRED,
                "$contextString 'location' is required and cannot be empty.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::location),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#operationReplyAddressObject",
            )
            return
        }
        if (!RUNTIME_EXPRESSION_GENERAL.matches(location)) {
            results.warn(
                OPERATION_REPLY_ADDRESS_FORMAT,
                "$contextString 'location' ('$location') does not appear to follow a valid runtime expression format.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::location),
            )
        }
    }
}
