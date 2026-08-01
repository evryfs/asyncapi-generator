package dev.banking.asyncapi.generator.core.model.exceptions

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic

sealed class AsyncApiParseException(message: String) : Exception(message) {

    class EmptyYamlFile(fileName: String) :
        AsyncApiParseException("Empty Yaml file : $fileName")

    class Mandatory(name: String, path: String, context: AsyncApiContext) :
        AsyncApiParseException(buildMessage("Missing mandatory '$name'", path, context))

    class UnexpectedSchemaFormat(format: String, path: String, context: AsyncApiContext) :
        AsyncApiParseException(buildMessage("SchemaFormat: $format is not valid.", path, context))

    class NativeSchemaAssetReadFailure(
        reference: String,
        path: String,
        context: AsyncApiContext,
        reason: String,
    ) : AsyncApiParseException(
            buildMessage(
                "Native schema asset '$reference' could not be read. Reason: $reason",
                path,
                context,
            ),
        )

    class UnexpectedValue(
        receivedValue: String,
        expectedValue: String,
        path: String,
        context: AsyncApiContext,
        actualValue: Any? = null,
    ) :
        AsyncApiParseException(
            buildMessage(
                ParserDiagnosticFormatter.unexpectedValueMessage(receivedValue, expectedValue, actualValue),
                path,
                context
            )
        )

    class ParserDiagnosticFailure(
        val diagnostic: ParserDiagnostic,
        context: AsyncApiContext,
    ) : AsyncApiParseException(ParserDiagnosticFormatter.format(diagnostic, context))

    companion object {
        private fun buildMessage(header: String, path: String, context: AsyncApiContext): String {
            val snippet = context.pathSnippet(path)
            val file = context.getCurrentFile()
            val fileName = file.name ?: "(unknown)"
            return buildString {
                appendLine(header)
                appendLine()
                appendLine(snippet.ifBlank { "→ $fileName ($path)" })
            }.trimEnd()
        }

    }
}
