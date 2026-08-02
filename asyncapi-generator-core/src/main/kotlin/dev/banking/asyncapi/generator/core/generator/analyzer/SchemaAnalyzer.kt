package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.schemas.Schema

class SchemaAnalyzer {

    private val referenceAnalyzer = ReferenceAnalyzer()
    private val enumTypeAnalyzer = EnumTypeAnalyzer()
    private val inlineSchemaAnalyzer = InlineSchemaAnalyzer()
    private val polymorphicAnalyzer = PolymorphicAnalyzer()

    fun analyze(schemas: Map<String, Schema>): Pair<Map<String, Schema>, Map<String, List<String>>> {
        val referencedSchemas = referenceAnalyzer.analyze(schemas)
        val generatorTypedSchemas = enumTypeAnalyzer.analyze(referencedSchemas)
        val inlinedSchemas = inlineSchemaAnalyzer.analyze(generatorTypedSchemas)
        val polymorphicSchemas = polymorphicAnalyzer.analyze(inlinedSchemas)
        return inlinedSchemas to polymorphicSchemas
    }
}
