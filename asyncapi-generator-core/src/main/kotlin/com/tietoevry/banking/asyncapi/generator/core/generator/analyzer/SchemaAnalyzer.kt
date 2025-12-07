package com.tietoevry.banking.asyncapi.generator.core.generator.analyzer

import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

class SchemaAnalyzer {

    private val referenceAnalyzer = ReferenceAnalyzer()
    private val inlineSchemaAnalyzer = InlineSchemaAnalyzer()

    // Stage that extracts metadata without modifying the schema map
    private val polymorphicAnalyzer = PolymorphicAnalyzer()

    fun analyze(schemas: Map<String, Schema>): Pair<Map<String, Schema>, Map<String, List<String>>> {
        val referencedSchemas = referenceAnalyzer.analyze(schemas)
        val inlinedSchemas = inlineSchemaAnalyzer.analyze(referencedSchemas)
        val polymorphicSchemas = polymorphicAnalyzer.analyze(inlinedSchemas)
        return inlinedSchemas to polymorphicSchemas
    }
}
