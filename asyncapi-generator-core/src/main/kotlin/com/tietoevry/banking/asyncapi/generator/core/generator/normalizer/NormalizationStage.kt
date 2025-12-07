package com.tietoevry.banking.asyncapi.generator.core.generator.normalizer

import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

interface NormalizationStage {
    fun normalize(schemas: Map<String, Schema>): Map<String, Schema>
}
