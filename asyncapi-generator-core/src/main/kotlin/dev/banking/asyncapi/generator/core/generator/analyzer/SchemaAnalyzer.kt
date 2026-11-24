package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * Prepares normalized schemas for model generation, in four steps: add the
 * schemas they reference, infer enum types, promote inline objects and enums to
 * named schemas, and record which unions each schema belongs to.
 */
internal class SchemaAnalyzer {
    private val referenceAnalyzer = ReferenceAnalyzer()
    private val enumTypeAnalyzer = EnumTypeAnalyzer()
    private val inlineSchemaAnalyzer = InlineSchemaAnalyzer()
    private val polymorphicAnalyzer = PolymorphicAnalyzer()

    /** The analyzed [schemas] with their union memberships. */
    fun analyze(schemas: Map<String, Schema>): SchemaAnalysis {
        val referencedSchemas = referenceAnalyzer.analyze(schemas)
        val generatorTypedSchemas = enumTypeAnalyzer.analyze(referencedSchemas)
        val inlinedSchemas = inlineSchemaAnalyzer.analyze(generatorTypedSchemas)
        val polymorphicRelationships = polymorphicAnalyzer.analyze(inlinedSchemas)
        return SchemaAnalysis(
            schemas = inlinedSchemas,
            polymorphicRelationships = polymorphicRelationships,
        )
    }
}
