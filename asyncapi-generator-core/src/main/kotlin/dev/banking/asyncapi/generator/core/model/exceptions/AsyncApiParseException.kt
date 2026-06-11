package dev.banking.asyncapi.generator.core.model.exceptions

import dev.banking.asyncapi.generator.core.context.AsyncApiContext

sealed class AsyncApiParseException(message: String) : Exception(message) {

    class EmptyYamlFile(fileName: String) :
        AsyncApiParseException("Empty Yaml file : $fileName")

    class Mandatory(name: String, path: String, context: AsyncApiContext) :
        AsyncApiParseException(buildMessage("Missing mandatory '$name'", path, context))

    class UnexpectedSchemaFormat(format: String, path: String, context: AsyncApiContext) :
        AsyncApiParseException(buildMessage("SchemaFormat: $format is not valid.", path, context))

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

        private fun scalarHint(
            expectedValue: String,
            actualValue: Any?,
        ): String? {
            val expected = expectedValue.lowercase()
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
