package dev.banking.asyncapi.generator.core.bundler.correlations

import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface

class CorrelationIdBundler {

    fun bundleMap(
        correlationIds: Map<String, CorrelationIdInterface>?,
        visited: Set<String>,
    ): Map<String, CorrelationIdInterface>? =
        correlationIds?.mapValues { (_, correlationId) ->
            when (correlationId) {
                is CorrelationIdInterface.CorrelationIdReference -> {
                    val ref = correlationId.reference.ref
                    if (visited.contains(ref)) {
                        correlationId
                    } else {
                        correlationId.reference.inline()
                        correlationId

                    }
                }
                else -> correlationId
            }
        }

    fun bundle(correlationId: CorrelationIdInterface, visited: Set<String>): CorrelationIdInterface {
        return when (correlationId) {
            is CorrelationIdInterface.CorrelationIdReference -> {
                val ref = correlationId.reference.ref
                if (visited.contains(ref)) {
                    correlationId
                } else {
                    correlationId.reference.inline()
                    correlationId
                }
            }
            else -> correlationId
        }
    }
}
