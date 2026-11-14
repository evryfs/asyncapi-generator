package dev.banking.asyncapi.generator.core.generator.ir

import dev.banking.asyncapi.generator.core.generator.output.GenerationResult

/** Renders language-neutral payload models as source artifacts of one language. */
internal fun interface ModelEmitter {
    fun emit(models: List<ModelDefinition>): GenerationResult
}
