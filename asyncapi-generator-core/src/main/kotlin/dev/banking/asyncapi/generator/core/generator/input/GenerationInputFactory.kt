package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.generator.analyzer.ChannelAnalyzer
import dev.banking.asyncapi.generator.core.generator.analyzer.SchemaAnalyzer
import dev.banking.asyncapi.generator.core.generator.loader.AsyncApiSchemaLoader
import dev.banking.asyncapi.generator.core.generator.normalizer.SchemaNormalizer
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument

/**
 * Prepares AsyncAPI documents for generator model creation.
 */
class GenerationInputFactory(
    private val schemaNormalizer: SchemaNormalizer = SchemaNormalizer(),
    private val schemaAnalyzer: SchemaAnalyzer = SchemaAnalyzer(),
    private val channelAnalyzer: ChannelAnalyzer = ChannelAnalyzer(),
) {
    fun create(asyncApiDocument: AsyncApiDocument): GenerationInput {
        val loadedSchemas = AsyncApiSchemaLoader.load(asyncApiDocument)
        val schemas = loadedSchemas.schemas
        val normalizedSchemas = schemaNormalizer.normalize(schemas)
        val schemaAnalysis = schemaAnalyzer.analyze(normalizedSchemas)
        val analyzedChannels = channelAnalyzer.analyze(asyncApiDocument)

        return GenerationInput(
            schemas = schemaAnalysis.schemas,
            declaredSchemas = loadedSchemas.allComponentSchemas,
            multiFormatSchemas = loadedSchemas.multiFormatSchemas,
            polymorphicRelationships = schemaAnalysis.polymorphicRelationships,
            channels = analyzedChannels,
        )
    }
}
