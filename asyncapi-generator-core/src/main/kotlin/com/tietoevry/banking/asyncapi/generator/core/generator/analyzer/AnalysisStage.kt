package com.tietoevry.banking.asyncapi.generator.core.generator.analyzer

import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

interface AnalysisStage<T> {
    fun analyze(schemas: Map<String, Schema>): T
}
