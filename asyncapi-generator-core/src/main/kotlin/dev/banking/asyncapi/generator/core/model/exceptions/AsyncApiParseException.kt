package dev.banking.asyncapi.generator.core.model.exceptions

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic

sealed class AsyncApiParseException(message: String) : Exception(message) {

    class ParserDiagnosticFailure internal constructor(
        val diagnostic: ParserDiagnostic,
        context: AsyncApiContext,
    ) : AsyncApiParseException(ParserDiagnosticFormatter.format(diagnostic, context))

    class UnexpectedSchemaFormat internal constructor(
        val format: String,
        val path: String,
        val sourceLocation: SourceLocation,
        context: AsyncApiContext,
    ) : AsyncApiParseException(buildMessage("SchemaFormat: $format is not valid.", path, context))

    class NativeSchemaAssetReadFailure internal constructor(
        val reference: String,
        val path: String,
        val sourceLocation: SourceLocation,
        context: AsyncApiContext,
        val reason: String,
    ) : AsyncApiParseException(
            buildMessage(
                "Native schema asset '$reference' could not be read. Reason: $reason",
                path,
                context,
            ),
        )

    companion object {
        private fun buildMessage(header: String, path: String, context: AsyncApiContext): String {
            val snippet = context.sourceTracking.pathSnippet(path)
            val file = context.sourceTracking.getCurrentFile()
            val fileName = file.name ?: "(unknown)"
            return buildString {
                appendLine(header)
                appendLine()
                appendLine(snippet.ifBlank { "→ $fileName ($path)" })
            }.trimEnd()
        }
    }
}
