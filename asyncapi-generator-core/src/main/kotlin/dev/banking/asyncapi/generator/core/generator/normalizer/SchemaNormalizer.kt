package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.schemas.Schema

class SchemaNormalizer {
    private val compositionNormalizer = CompositionNormalizer()
    private val conditionalNormalizer = ConditionalNormalizer()

    fun normalize(initialSchemas: Map<String, Schema>): Map<String, Schema> =
        conditionalNormalizer.normalize(
            compositionNormalizer.normalize(initialSchemas),
        )
}
