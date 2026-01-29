package dev.banking.asyncapi.generator.core.validator.correlations

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.RUNTIME_EXPRESSION_GENERAL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class CorrelationIdValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: CorrelationIdInterface, contextString: String, results: ValidationResults) {
        when (node) {
            is CorrelationIdInterface.CorrelationIdInline ->
                validate(node.correlationId, contextString, results)
            is CorrelationIdInterface.CorrelationIdReference ->
                referenceResolver.resolve(node.reference, contextString, results)
        }
    }

    fun validate(node: CorrelationId, contextString: String, results: ValidationResults) {
        validateLocation(node, contextString, results)
    }

    private fun validateLocation(node: CorrelationId, contextString: String, results: ValidationResults) {
        val location = node.location.let(::sanitizeString)
        if (location.isBlank()) {
            results.error(
                "$contextString 'location' is required and cannot be empty.",
                asyncApiContext.getLine(node, node::location)
            )
            return
        }
        // Basic syntax check for runtime expressions, e.g. "$message.header#/correlationId"
        if (!RUNTIME_EXPRESSION_GENERAL.matches(location)) {
            results.warn(
                "$contextString 'location' ('$location') does not follow valid runtime expression.",
                asyncApiContext.getLine(node, node::location),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#correlationIdObject"
            )
        }
    }
}
