package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.schemas.Schema

interface NormalizationStage {
    fun normalize(schemas: Map<String, Schema>): Map<String, Schema>
}
