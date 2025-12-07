package dev.banking.asyncapi.generator.core.validator.operations

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

    fun validateInterface(
        operationReplyAddressName: String,
        node: OperationReplyAddressInterface,
        results: ValidationResults,
    ) {
        when (node) {
            is OperationReplyAddressInterface.OperationReplyAddressInline ->
                validate(node.operationReplyAddress, operationReplyAddressName, results)

            is OperationReplyAddressInterface.OperationReplyAddressReference ->
                referenceResolver.resolve(operationReplyAddressName, node.reference, "Operation Reply Address", results)
        }
    }

    fun validate(node: OperationReplyAddress, operationReplyAddressName: String, results: ValidationResults) {
        validateDescription(node, operationReplyAddressName, results)
        validateLocation(node, operationReplyAddressName, results)
    }

    private fun validateDescription(
        node: OperationReplyAddress,
        operationReplyAddressName: String,
        results: ValidationResults,
    ) {
        val description = node.description?.let(::sanitizeString)
        description?.length?.let {
            if (it < 3) {
                results.warn(
                    "Operation reply address '$operationReplyAddressName' 'description' is too short to be meaningful.",
                    asyncApiContext.getLine(node, node::description)
                )
            }
        }
    }

    private fun validateLocation(
        node: OperationReplyAddress,
        operationReplyAddressName: String,
        results: ValidationResults,
    ) {
        val location = node.location.let(::sanitizeString)
        if (location.isBlank()) {
            results.error(
                "Operation reply address '$operationReplyAddressName' 'location' is required and cannot be empty.",
                asyncApiContext.getLine(node, node::location)
            )
            return
        }
        val runtimeExprRegex = Regex("""^\$[a-zA-Z]+\.[a-zA-Z0-9_/#]+$""")
        if (!runtimeExprRegex.matches(location)) {
            results.warn(
                "Operation reply address '$operationReplyAddressName' 'location' ('$location') does not appear to " +
                    "follow a valid runtime expression format.",
                asyncApiContext.getLine(node, node::location)
            )
        }
    }
}
