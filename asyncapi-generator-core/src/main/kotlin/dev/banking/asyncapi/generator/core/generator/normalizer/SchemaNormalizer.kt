package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.schemas.Schema

/** Produces the static schema view consumed by generator model analysis. */
internal class SchemaNormalizer {
    private val compositionNormalizer = CompositionNormalizer()
    private val conditionalNormalizer = ConditionalNormalizer()

    /** [initialSchemas] with `allOf` flattened, then if/then/else projected into plain properties. */
    fun normalize(initialSchemas: Map<String, Schema>): Map<String, Schema> =
        conditionalNormalizer.normalize(
            compositionNormalizer.normalize(initialSchemas),
        )
}
