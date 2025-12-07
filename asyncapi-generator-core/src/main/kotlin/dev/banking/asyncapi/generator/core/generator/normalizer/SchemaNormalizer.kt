package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * Orchestrates the schema normalization phase. It maintains a registry of [NormalizationStage]s
 * and executes them in order to simplify and canonicalize the schema definitions.
 */
class SchemaNormalizer {

    private val stages: List<NormalizationStage> = listOf(
        CompositionNormalizer(),
        ConditionalNormalizer()
    )

    /**
     * Runs all normalization stages on the provided schemas.
     * @param initialSchemas The raw schemas loaded from the document.
     * @return A map of normalized, simplified schemas.
     */
    fun normalize(initialSchemas: Map<String, Schema>): Map<String, Schema> {
        return stages.fold(initialSchemas) { currentSchemas, stage ->
            stage.normalize(currentSchemas)
        }
    }
}
