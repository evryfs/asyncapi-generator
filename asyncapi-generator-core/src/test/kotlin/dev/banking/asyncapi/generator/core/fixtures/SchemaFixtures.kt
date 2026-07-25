package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Shared schema builders for focused generator contract tests.
 */
internal object SchemaFixtures {
    fun inline(
        type: Any,
        format: String? = null,
        description: String? = null,
        multipleOf: Number? = null,
    ): SchemaInterface.SchemaInline =
        SchemaInterface.SchemaInline(
            Schema(
                type = type,
                format = format,
                description = description,
                multipleOf = multipleOf,
            ),
        )
}
