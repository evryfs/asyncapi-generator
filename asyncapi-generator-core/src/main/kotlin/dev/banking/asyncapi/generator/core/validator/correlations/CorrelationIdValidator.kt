package dev.banking.asyncapi.generator.core.validator.correlations

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.RUNTIME_EXPRESSION_GENERAL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CORRELATION_LOCATION_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CORRELATION_LOCATION_REQUIRED
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class CorrelationIdValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: CorrelationIdInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is CorrelationIdInterface.CorrelationIdInline ->
                validate(node.correlationId, contextString, results)
            is CorrelationIdInterface.CorrelationIdReference ->
                referenceResolver.resolve(node.reference, contextString, results)
        }
    }

    fun validate(node: CorrelationId, contextString: String, results: ValidationCollector) {
        validateLocation(node, contextString, results)
    }

    private fun validateLocation(node: CorrelationId, contextString: String, results: ValidationCollector) {
        val location = node.location.let(::sanitizeString)
        if (location.isBlank()) {
            results.error(
                CORRELATION_LOCATION_REQUIRED,
                "$contextString 'location' is required and cannot be empty.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::location),
            )
            return
        }
        // Basic syntax check for runtime expressions, e.g. "$message.header#/correlationId"
        if (!RUNTIME_EXPRESSION_GENERAL.matches(location)) {
            results.warn(
                CORRELATION_LOCATION_FORMAT,
                "$contextString 'location' ('$location') does not follow valid runtime expression.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::location),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#correlationIdObject",
            )
        }
    }
}
