package dev.banking.asyncapi.generator.core.model.exceptions

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic

sealed class AsyncApiParseException(message: String) : Exception(message) {

    class EmptyYamlFile(fileName: String) :
        AsyncApiParseException("Empty Yaml file : $fileName")

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
                unexpectedValueMessage(receivedValue, expectedValue, actualValue),
                path,
                context
            )
        )

    class ParserDiagnosticFailure(
        val diagnostic: ParserDiagnostic,
        context: AsyncApiContext,
    ) : AsyncApiParseException(formatDiagnostic(diagnostic, context))

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

        private fun unexpectedValueMessage(
            receivedValue: String,
            expectedValue: String,
            actualValue: Any?,
        ): String {
            val actualDescription = actualValue?.let { " ${formatValue(it)}" } ?: ""
            val expected = if (expectedValue.isNotEmpty()) expectedValue else "supported value"
            val hint = scalarHint(expectedValue, actualValue)
            return buildString {
                append("Unexpected value: expected $expected, found $receivedValue$actualDescription.")
                if (hint != null) {
                    appendLine()
                    append(hint)
                }
            }
        }

        private fun formatDiagnostic(
            diagnostic: ParserDiagnostic,
            context: AsyncApiContext,
        ): String =
            when (diagnostic) {
                is ParserDiagnostic.MissingRequiredMember ->
                    buildMessage(
                        header = "Missing required member '${diagnostic.memberName}'.",
                        diagnostic = diagnostic,
                        context = context,
                    )

                is ParserDiagnostic.UnexpectedValueType ->
                    buildMessage(
                        header = unexpectedValueMessage(
                            receivedValue = diagnostic.actualType?.simpleName ?: "null",
                            expectedValue = renderType(diagnostic.expectedType.toString()),
                            actualValue = diagnostic.actualValue,
                        ),
                        diagnostic = diagnostic,
                        context = context,
                    )
            }

        private fun buildMessage(
            header: String,
            diagnostic: ParserDiagnostic,
            context: AsyncApiContext,
        ): String {
            val snippet = context.pathSnippet(diagnostic.path)
            val fileName = diagnostic.sourceLocation.file.name
            return buildString {
                appendLine(header)
                appendLine()
                appendLine(snippet.ifBlank { "→ $fileName (${diagnostic.path})" })
            }.trimEnd()
        }

        private fun renderType(type: String): String =
            type
                .replace("kotlin.collections.", "")
                .replace("kotlin.", "")

        private fun scalarHint(
            expectedValue: String,
            actualValue: Any?,
        ): String? {
            val expected = expectedValue.lowercase().removeSuffix("?")
            return when (expected) {
                "boolean" if actualValue is String && actualValue.isJsonBooleanText() ->
                    "Hint: quoted booleans are strings in YAML. Use true or false without quotes when the field expects a boolean."

                "boolean" if actualValue is String && actualValue.isYaml11BooleanWord() ->
                    "Hint: AsyncAPI uses JSON-compatible booleans. Use true or false for booleans; values like yes, no, on, and off are strings."

                "number" if actualValue is String && actualValue.isNumberText() ->
                    "Hint: quoted numbers are strings in YAML. Remove the quotes when the field expects a number."

                "string" if (actualValue is Boolean || actualValue is Number) ->
                    "Hint: quote the value if the field should contain text instead of a ${actualValue::class.simpleName}."

                else -> null
            }
        }

        private fun formatValue(value: Any): String =
            when (value) {
                is String -> "\"$value\""
                else -> value.toString()
            }

        private fun String.isJsonBooleanText(): Boolean =
            equals("true", ignoreCase = true) || equals("false", ignoreCase = true)

        private fun String.isYaml11BooleanWord(): Boolean =
            lowercase() in setOf("yes", "no", "on", "off")

        private fun String.isNumberText(): Boolean =
            toDoubleOrNull() != null
    }
}
