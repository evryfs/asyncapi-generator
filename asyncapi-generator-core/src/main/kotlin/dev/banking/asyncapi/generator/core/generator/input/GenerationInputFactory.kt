package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.generator.analyzer.ChannelAnalyzer
import dev.banking.asyncapi.generator.core.generator.analyzer.SchemaAnalyzer
import dev.banking.asyncapi.generator.core.generator.loader.AsyncApiSchemaLoader
import dev.banking.asyncapi.generator.core.generator.normalizer.SchemaNormalizer
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument

/**
 * Prepares AsyncAPI documents for generator model creation.
 *
 * Expected behavior is covered by:
 * - `GenerationInputFactoryTest`
 */
class GenerationInputFactory(
    private val schemaNormalizer: SchemaNormalizer = SchemaNormalizer(),
    private val schemaAnalyzer: SchemaAnalyzer = SchemaAnalyzer(),
    private val channelAnalyzer: ChannelAnalyzer = ChannelAnalyzer(),
) {
    fun create(asyncApiDocument: AsyncApiDocument): GenerationInput {
        val schemas = AsyncApiSchemaLoader.load(asyncApiDocument)
        val multiFormatSchemas = AsyncApiSchemaLoader.loadMultiFormatSchemas(asyncApiDocument)
        val normalizedSchemas = schemaNormalizer.normalize(schemas)
        val (analyzedSchemas, polymorphicRelationships) = schemaAnalyzer.analyze(normalizedSchemas)
        val analyzedChannels = channelAnalyzer.analyze(asyncApiDocument).channels

        return GenerationInput(
            schemas = analyzedSchemas,
            multiFormatSchemas = multiFormatSchemas,
            polymorphicRelationships = polymorphicRelationships,
            channels = analyzedChannels,
        )
    }
}
