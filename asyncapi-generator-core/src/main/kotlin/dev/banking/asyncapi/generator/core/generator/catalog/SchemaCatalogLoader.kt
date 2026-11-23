package dev.banking.asyncapi.generator.core.generator.catalog

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument

/**
 * Collects the schemas of a bundled document under their generated names.
 *
 * Component schemas come first, then message payloads and Kafka keys, so a
 * component schema keeps its own name when a message reaches it too.
 */
internal object SchemaCatalogLoader {
    /** The source models and contract declarations of [asyncApiDocument]. */
    fun load(asyncApiDocument: AsyncApiDocument): LoadedSchemas {
        val declarations = SchemaDeclarations()
        val usage = SchemaUsageIndex.of(asyncApiDocument)
        ComponentSchemaCollector.collect(asyncApiDocument.components, usage, declarations)
        MessageSchemaCollector.collect(asyncApiDocument, declarations)
        return declarations.toLoadedSchemas()
    }
}
