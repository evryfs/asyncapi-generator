package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.document.SourceLocation
import java.io.File
import java.io.IOException

/**
 * Enforces resource limits during document loading.
 *
 * @param limits configurable thresholds for document sizes, external references, and schema reads
 */
internal class ResourceBudget(
    private val limits: ParserLoadResourceLimits,
) {

    private val budget = ParserLoadResourceBudget(limits)

    fun registerDocument(
        file: File,
        content: String,
        location: SourceLocation,
    ): ResourceBudgetResult<Unit> = enforceLimits(location) {
        budget.registerDocument(
            file = file,
            sourceBytes = content.toByteArray(Charsets.UTF_8).size.toLong(),
        )
    }

    fun registerExternalDocument(
        file: File,
        location: SourceLocation,
    ): ResourceBudgetResult<Unit> = enforceLimits(location) {
        budget.registerExternalDocument(file)
    }

    fun registerReferenceTarget(
        file: File,
        pointer: String,
        location: SourceLocation,
    ): ResourceBudgetResult<Unit> = enforceLimits(location) {
        budget.registerReferenceTarget(file, pointer)
    }

    fun <T> withinExternalReference(
        location: SourceLocation,
        block: () -> T,
    ): ResourceBudgetResult<T> = enforceLimits(location) {
        budget.withinExternalReference(block)
    }

    @Throws(IOException::class)
    fun readNativeSchemaAsset(
        file: File,
        location: SourceLocation,
        path: String,
    ): ResourceBudgetResult<String> = enforceLimits(location, path) {
        budget.readNativeSchemaAsset(file)
    }

    fun sourceFiles(): Set<File> = budget.sourceFiles()

    private fun <T> enforceLimits(
        location: SourceLocation,
        path: String = location.path,
        block: () -> T,
    ): ResourceBudgetResult<T> =
        try {
            ResourceBudgetResult.Success(block())
        } catch (exception: ParserLoadResourceLimitExceeded) {
            ResourceBudgetResult.LimitExceeded(
                limit = exception.limit,
                maximum = exception.maximum,
                observed = exception.observed,
                path = path,
                sourceLocation = location,
            )
        }
}
