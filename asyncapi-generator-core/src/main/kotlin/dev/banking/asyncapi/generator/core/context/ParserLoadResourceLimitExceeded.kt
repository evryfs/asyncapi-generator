package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserLoadResourceLimit

/**
 * Thrown when a resource budget operation exceeds a configured limit.
 *
 * @param limit the type of limit that was exceeded
 * @param maximum the configured maximum value
 * @param observed the actual value that exceeded the limit
 */
internal class ParserLoadResourceLimitExceeded(
    val limit: ParserLoadResourceLimit,
    val maximum: Long,
    val observed: Long,
) : RuntimeException("Parser load exceeded ${limit.displayName} limit of $maximum: observed $observed")
