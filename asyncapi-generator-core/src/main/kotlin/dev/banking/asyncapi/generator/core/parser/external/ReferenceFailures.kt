package dev.banking.asyncapi.generator.core.parser.external

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.source.SourceLocation
import java.io.File
import java.io.IOException

/** Runs [access] to [file] and reports an I/O or security failure as a missing document. */
internal fun <T> AsyncApiContext.guardFileAccess(
    reference: Reference,
    file: File,
    access: () -> T,
): T =
    try {
        access()
    } catch (exception: IOException) {
        throw missingDocument(reference, file, exception)
    } catch (exception: SecurityException) {
        throw missingDocument(reference, file, exception)
    }

/** The failure for a `$ref` that is not a valid reference, for the given [reason]. */
internal fun AsyncApiContext.invalidReference(
    reference: Reference,
    reason: String,
    cause: Exception,
): AsyncApiParseException =
    failure(
        ParserDiagnostic.InvalidReference(
            reference = reference.ref,
            reason = reason.trimEnd('.'),
            path = referenceLocation(reference).path,
            sourceLocation = referenceLocation(reference),
        ),
        cause,
    )

/** The failure for a `$ref` whose document [file] cannot be read. */
internal fun AsyncApiContext.missingDocument(
    reference: Reference,
    file: File,
    cause: Exception? = null,
): AsyncApiParseException =
    failure(
        ParserDiagnostic.ReferenceDocumentNotFound(
            reference = reference.ref,
            resolvedFile = file.absolutePath,
            path = referenceLocation(reference).path,
            sourceLocation = referenceLocation(reference),
        ),
        cause,
    )

/** The failure for a `$ref` whose pointer points at nothing in its document. */
internal fun AsyncApiContext.missingTarget(reference: Reference): AsyncApiParseException =
    failure(
        ParserDiagnostic.ReferenceTargetNotFound(
            reference = reference.ref,
            path = referenceLocation(reference).path,
            sourceLocation = referenceLocation(reference),
        ),
    )

/** The location of the `$ref` member of [reference], or of the reference object itself. */
internal fun AsyncApiContext.referenceLocation(reference: Reference): SourceLocation =
    requireNotNull(
        modelTracking.getSourceLocation(reference, $$"$ref") ?: modelTracking.getSourceLocation(reference),
    ) { "Reference '${reference.ref}' was not registered with a source location" }

private fun AsyncApiContext.failure(
    diagnostic: ParserDiagnostic,
    cause: Exception? = null,
): AsyncApiParseException =
    AsyncApiParseException
        .ParserDiagnosticFailure(diagnostic, this)
        .also { failure -> cause?.let(failure::initCause) }
