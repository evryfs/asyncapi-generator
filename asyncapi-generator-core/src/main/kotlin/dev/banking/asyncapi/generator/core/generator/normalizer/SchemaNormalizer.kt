package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.schemas.Schema

class SchemaNormalizer {

    private val stages: List<NormalizationStage> = listOf(
        CompositionNormalizer(),
        ConditionalNormalizer()
    )

    fun normalize(initialSchemas: Map<String, Schema>): Map<String, Schema> {
        return stages.fold(initialSchemas) { currentSchemas, stage ->
            stage.normalize(currentSchemas)
        }
    }
}
