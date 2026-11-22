package dev.banking.asyncapi.generator.core.bundler.correlations

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface

/**
 * Bundles correlation ID objects and references.
 */
internal class CorrelationIdBundler {
    fun bundleMap(
        correlationIds: Map<String, CorrelationIdInterface>?,
        scope: BundlingScope,
    ): Map<String, CorrelationIdInterface>? =
        correlationIds?.mapValues { (_, correlationId) ->
            bundle(correlationId, scope)
        }

    fun bundle(
        correlationId: CorrelationIdInterface,
        scope: BundlingScope,
    ): CorrelationIdInterface =
        when (correlationId) {
            is CorrelationIdInterface.CorrelationIdReference -> {
                correlationId.copy(reference = ReferenceBundler.inlineIfUnvisited(correlationId.reference, scope))
            }
            else -> correlationId
        }
}
