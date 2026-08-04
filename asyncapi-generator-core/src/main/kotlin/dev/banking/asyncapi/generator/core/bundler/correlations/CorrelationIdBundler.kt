package dev.banking.asyncapi.generator.core.bundler.correlations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface

/**
 * Bundles correlation ID objects and references.
 *
 * Expected behavior is covered by:
 * - `CorrelationIdBundlerTest`
 */
internal class CorrelationIdBundler {
    fun bundleMap(
        correlationIds: Map<String, CorrelationIdInterface>?,
        context: BundlingContext,
    ): Map<String, CorrelationIdInterface>? =
        correlationIds?.mapValues { (_, correlationId) ->
            bundle(correlationId, context)
        }
    fun bundle(correlationId: CorrelationIdInterface, context: BundlingContext): CorrelationIdInterface =
        when (correlationId) {
            is CorrelationIdInterface.CorrelationIdReference -> {
                ReferenceBundler.inlineIfUnvisited(correlationId.reference, context)
                correlationId
            }
            else -> correlationId
        }
}
