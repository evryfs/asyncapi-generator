package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserLoadResourceLimit
import dev.banking.asyncapi.generator.core.source.SourceLocation

/**
 * Result of a resource budget operation.
 */
internal sealed interface ResourceBudgetResult<out T> {
    data class Success<T>(
        val value: T,
    ) : ResourceBudgetResult<T>

    data class LimitExceeded(
        val limit: ParserLoadResourceLimit,
        val maximum: Long,
        val observed: Long,
        val path: String,
        val sourceLocation: SourceLocation,
    ) : ResourceBudgetResult<Nothing>
}
