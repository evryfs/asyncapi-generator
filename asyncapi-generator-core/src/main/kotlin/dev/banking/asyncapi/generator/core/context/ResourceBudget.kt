package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import java.io.File
import java.io.IOException

/**
 * Enforces resource limits during document loading.
 *
 * @param limits configurable thresholds for document sizes, external references, and schema reads
 * @param context owning context used for error reporting when limits are exceeded
 */
internal class ResourceBudget(
    private val limits: ParserLoadResourceLimits,
    private val context: AsyncApiContext,
) {

    private val budget = ParserLoadResourceBudget(limits)

    fun registerDocument(
        file: File,
        content: String,
        location: SourceLocation,
    ) {
        enforceLimits(location) {
            budget.registerDocument(
                file = file,
                sourceBytes = content.toByteArray(Charsets.UTF_8).size.toLong(),
            )
        }
    }

    fun registerExternalDocument(
        file: File,
        location: SourceLocation,
    ) {
        enforceLimits(location) {
            budget.registerExternalDocument(file)
        }
    }

    fun registerReferenceTarget(
        file: File,
        pointer: String,
        location: SourceLocation,
    ) {
        enforceLimits(location) {
            budget.registerReferenceTarget(file, pointer)
        }
    }

    fun <T> withinExternalReference(
        location: SourceLocation,
        block: () -> T,
    ): T = enforceLimits(location) {
        budget.withinExternalReference(block)
    }

    @Throws(IOException::class)
    fun readNativeSchemaAsset(
        file: File,
        location: SourceLocation,
        path: String,
    ): String = enforceLimits(location, path) {
        budget.readNativeSchemaAsset(file)
    }

    fun sourceFiles(): Set<File> = budget.sourceFiles()

    private fun <T> enforceLimits(
        location: SourceLocation,
        path: String = location.path,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (exception: ParserLoadResourceLimitExceeded) {
            throw AsyncApiParseException.ParserDiagnosticFailure(
                diagnostic = ParserDiagnostic.LoadResourceLimitExceeded(
                    limit = exception.limit,
                    maximum = exception.maximum,
                    observed = exception.observed,
                    path = path,
                    sourceLocation = location,
                ),
                context = context,
            )
        }
}
