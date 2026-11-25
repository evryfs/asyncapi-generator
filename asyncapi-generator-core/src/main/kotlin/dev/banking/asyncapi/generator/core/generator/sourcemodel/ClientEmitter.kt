package dev.banking.asyncapi.generator.core.generator.sourcemodel

import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult

/** Renders messaging clients for analyzed channels as source artifacts of one language. */
internal fun interface ClientEmitter {
    fun emit(channels: List<AnalyzedChannel>): GenerationResult
}
