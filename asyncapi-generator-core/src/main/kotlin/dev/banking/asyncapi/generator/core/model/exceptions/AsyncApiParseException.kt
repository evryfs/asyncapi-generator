package dev.banking.asyncapi.generator.core.model.exceptions

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic

/** A failure to load the documents of an AsyncAPI contract. */
internal sealed class AsyncApiParseException(
    message: String,
) : Exception(message) {
    /**
     * A load failed with a structured [diagnostic].
     *
     * @property diagnostic what failed and where
     */
    class ParserDiagnosticFailure internal constructor(
        val diagnostic: ParserDiagnostic,
        message: String,
    ) : AsyncApiParseException(message)
}
