package dev.banking.asyncapi.generator.core.loader

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.validator.util.ValidationFindingFormatter
import java.io.File
import java.util.Collections.unmodifiableList
import java.util.Collections.unmodifiableSet

/**
 * Result of loading and validating one AsyncAPI document.
 *
 * Validation errors are thrown before a result is returned. Warnings remain
 * available to the caller and can be rendered with their source snippets by
 * calling [formatWarnings]. [sourceFiles] contains the canonical root,
 * external-document, and native-schema files consumed by this load.
 */
class AsyncApiDocumentLoadResult internal constructor(
    val document: AsyncApiDocument,
    warnings: List<ValidationFinding>,
    sourceFiles: Set<File>,
    private val context: AsyncApiContext,
) {
    /** Validation warnings collected from the root document and external fragments. */
    val warnings: List<ValidationFinding> = unmodifiableList(warnings.toList())

    /** Canonical source files consumed by this load, in deterministic path order. */
    val sourceFiles: Set<File> = unmodifiableSet(
        sourceFiles
            .map(File::getCanonicalFile)
            .sortedBy(File::getAbsolutePath)
            .toCollection(linkedSetOf()),
    )

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
