package dev.banking.asyncapi.generator.core.generator.ir

import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.ReservedModelName

/** Renders language-neutral payload models as source artifacts of one language. */
internal fun interface ModelEmitter {
    fun emit(models: List<ModelDefinition>): GenerationResult
}

/**
 * Fails on the first model named like one of [reservedNames], the types that generated [language]
 * sources use without qualification.
 */
internal fun List<ModelDefinition>.rejectReservedNames(
    language: String,
    reservedNames: Set<String>,
) {
    firstOrNull { model -> model.name in reservedNames }?.let { model -> throw ReservedModelName(model.name, language) }
}
