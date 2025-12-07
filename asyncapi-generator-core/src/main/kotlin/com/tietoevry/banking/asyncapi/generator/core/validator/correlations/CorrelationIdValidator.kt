package com.tietoevry.banking.asyncapi.generator.core.validator.correlations

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.correlations.CorrelationId
import com.tietoevry.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import com.tietoevry.banking.asyncapi.generator.core.resolver.ReferenceResolver
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidationResults
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class CorrelationIdValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(correlationIdName: String, node: CorrelationIdInterface, results: ValidationResults) {
        when (node) {
            is CorrelationIdInterface.CorrelationIdInline ->
                validate(node.correlationId, correlationIdName, results)
            is CorrelationIdInterface.CorrelationIdReference ->
                referenceResolver.resolve(correlationIdName, node.reference, "CorrelationId", results)
        }
    }

    fun validate(node: CorrelationId, correlationIdName: String, results: ValidationResults) {
        validateDescription(node, correlationIdName, results)
        validateLocation(node, correlationIdName, results)
    }

    private fun validateDescription(node: CorrelationId, correlationIdName: String, results: ValidationResults) {
        val description = node.description?.let(::sanitizeString)
        description?.length?.let {
            if (it < 3) {
                results.warn(
                    "CorrelationId '$correlationIdName' description is too short to be meaningful.",
                    asyncApiContext.getLine(node, node::description)
                )
            }
        }
    }

    private fun validateLocation(node: CorrelationId, correlationIdName: String, results: ValidationResults) {
        val location = node.location.let(::sanitizeString)
        if (location.isBlank()) {
            results.error(
                "CorrelationId '$correlationIdName' 'location' is required and cannot be empty.",
                asyncApiContext.getLine(node, node::location)
            )
            return
        }

        // Basic syntax check for runtime expressions, e.g. "$message.header#/correlationId"
        val runtimeExprRegex = Regex("""^\$[a-zA-Z]+\.[a-zA-Z0-9_/#]+$""")
        if (!runtimeExprRegex.matches(location)) {
            results.warn(
                "CorrelationId '$correlationIdName' 'location' ('$location') does not follow valid runtime expression.",
                asyncApiContext.getLine(node, node::location)
            )
        }
    }
}
