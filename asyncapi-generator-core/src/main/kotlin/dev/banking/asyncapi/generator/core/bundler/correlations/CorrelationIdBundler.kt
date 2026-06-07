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
class CorrelationIdBundler {

    fun bundleMap(
        correlationIds: Map<String, CorrelationIdInterface>?,
        visited: Set<String>,
    ): Map<String, CorrelationIdInterface>? =
        bundleMap(correlationIds, BundlingContext.from(visited))

    fun bundleMap(
        correlationIds: Map<String, CorrelationIdInterface>?,
        context: BundlingContext,
    ): Map<String, CorrelationIdInterface>? =
        correlationIds?.mapValues { (_, correlationId) ->
            bundle(correlationId, context)
        }

    fun bundle(correlationId: CorrelationIdInterface, visited: Set<String>): CorrelationIdInterface =
        bundle(correlationId, BundlingContext.from(visited))

    fun bundle(correlationId: CorrelationIdInterface, context: BundlingContext): CorrelationIdInterface =
        when (correlationId) {
            is CorrelationIdInterface.CorrelationIdReference -> {
                ReferenceBundler.inlineIfUnvisited(correlationId.reference, context)
                correlationId
            }
            else -> correlationId
        }
}
