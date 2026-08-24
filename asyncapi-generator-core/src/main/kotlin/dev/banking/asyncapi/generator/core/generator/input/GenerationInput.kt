package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * Analyzed AsyncAPI data consumed by language and schema generators.
 *
 * [GenerationInput] is produced after loading, normalizing, and analyzing the
 * AsyncAPI document. It does not contain rendered artifacts or write targets.
 *
 * [schemas] contains the normalized and analyzed view used by source generators.
 * [schemaDeclarations] retains the contract view used by schema artifact
 * generators that must preserve JSON Schema semantics.
 */
data class GenerationInput(
    val schemas: Map<String, Schema>,
    val schemaDeclarations: SchemaDeclarationCatalog = SchemaDeclarationCatalog(asyncApiSchemas = schemas),
    val polymorphicRelationships: Map<String, List<String>>,
    val channels: List<AnalyzedChannel>,
) {
    val declaredSchemas: Map<String, Schema>
        get() = schemaDeclarations.asyncApiSchemas

    val multiFormatSchemas: Map<String, MultiFormatSchema>
        get() = schemaDeclarations.multiFormatSchemas

    val schemaContext: GeneratorContext = GeneratorContext(schemas)

    fun schemaContextWith(additionalSchemas: Map<String, Schema>): GeneratorContext =
        if (additionalSchemas.isEmpty()) {
            schemaContext
        } else {
            GeneratorContext(schemas + additionalSchemas)
        }
}
