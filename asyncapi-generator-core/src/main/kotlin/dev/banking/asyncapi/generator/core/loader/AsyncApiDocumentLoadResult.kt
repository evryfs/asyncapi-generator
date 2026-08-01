package dev.banking.asyncapi.generator.core.loader

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.validator.util.ValidationFindingFormatter

/**
 * Result of loading and validating one AsyncAPI document.
 *
 * Validation errors are thrown before a result is returned. Warnings remain
 * available to the caller and can be rendered with their source snippets by
 * calling [formatWarnings].
 */
class AsyncApiDocumentLoadResult internal constructor(
    val document: AsyncApiDocument,
    warnings: List<ValidationFinding>,
    private val context: AsyncApiContext,
) {
    val warnings: List<ValidationFinding> = warnings.toList()

    fun formatWarnings(): String =
        if (warnings.isEmpty()) {
            ""
        } else {
            ValidationFindingFormatter.format(
                title = "Validation found ${warnings.size} warning(s):",
                findings = warnings,
                asyncApiContext = context,
            )
        }
}
