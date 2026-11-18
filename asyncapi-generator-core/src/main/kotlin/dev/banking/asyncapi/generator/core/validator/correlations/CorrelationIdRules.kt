package dev.banking.asyncapi.generator.core.validator.correlations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CORRELATION_LOCATION_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CORRELATION_LOCATION_REQUIRED
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.util.requireNonBlank
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Correlation ID Object.
 *
 * @param asyncApiContext the load the correlation IDs belong to
 */
internal class CorrelationIdRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<CorrelationId> {
    override fun check(
        node: CorrelationId,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val location = node.location
        val locationSource = asyncApiContext.modelTracking.getSourceLocation(node, node::location)
        if (!walk.results.requireNonBlank(location, "location", path, CORRELATION_LOCATION_REQUIRED, locationSource)) {
            return
        }
        if (!ValidationFormats.isRuntimeExpression(location)) {
            walk.results.error(
                CORRELATION_LOCATION_FORMAT,
                "$path 'location' ('$location') does not follow valid runtime expression.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::location),
            )
        }
    }
}
