package dev.banking.asyncapi.generator.core.generator.ir

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult

/** Renders messaging clients for analyzed channels as source artifacts of one language. */
fun interface ClientEmitter {
    fun emit(channels: List<AnalyzedChannel>): GenerationResult
}
