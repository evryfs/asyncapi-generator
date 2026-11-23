package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.bundler.schemas.PromotedSchemaRegistry
import dev.banking.asyncapi.generator.core.bundler.schemas.SchemaRecursionAnalyzer
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * What one bundling of a document shares across all of its paths: the schemas promoted into
 * `components.schemas` and which schemas are recursive.
 *
 * @param rootSchemas the component schemas of the root document, which keep their names in the bundle
 */
internal class BundlingRun(
    rootSchemas: Map<String, SchemaInterface> = emptyMap(),
) {
    /** The schemas promoted into the bundled `components.schemas`, by name and by model. */
    val schemaPromotions = PromotedSchemaRegistry(rootSchemas)

    /** Which schema models take part in a reference cycle. */
    val schemaRecursion = SchemaRecursionAnalyzer()

    /** The scope at the root of the document, before any reference is followed. */
    fun rootScope(): BundlingScope = BundlingScope.root(this)
}
