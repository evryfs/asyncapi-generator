package dev.banking.asyncapi.generator.core.model.exceptions

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic

internal object ParserDiagnosticFormatter {

    fun format(
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
                    header = unexpectedValueMessage(diagnostic),
                    diagnostic = diagnostic,
                    context = context,
                )

            is ParserDiagnostic.UnexpectedObjectMember ->
                buildMessage(
                    header =
                        "Unexpected member '${diagnostic.memberName}': " +
                            "expected ${diagnostic.expectedType}.",
                    diagnostic = diagnostic,
                    context = context,
                )

            is ParserDiagnostic.InvalidSpecificationVersion ->
                buildMessage(
                    header =
                        "Invalid AsyncAPI specification version '${diagnostic.declaredVersion}': " +
                            "expected major.minor.patch with an optional alphanumeric suffix.",
                    diagnostic = diagnostic,
                    context = context,
                )

            is ParserDiagnostic.UnsupportedSpecificationVersion ->
                buildMessage(
                    header = unsupportedVersionMessage(diagnostic),
                    diagnostic = diagnostic,
                    context = context,
                )

            is ParserDiagnostic.InvalidReference ->
                buildMessage(
                    header = "Invalid reference '${diagnostic.reference}': ${diagnostic.reason}.",
                    diagnostic = diagnostic,
                    context = context,
                )

            is ParserDiagnostic.ReferenceDocumentNotFound ->
                buildMessage(
                    header =
                        "Reference document for '${diagnostic.reference}' was not found or is not readable: " +
                            diagnostic.resolvedFile,
                    diagnostic = diagnostic,
                    context = context,
                )

            is ParserDiagnostic.ReferenceTargetNotFound ->
                buildMessage(
                    header = "Reference target '${diagnostic.reference}' was not found.",
                    diagnostic = diagnostic,
                    context = context,
                )
        }

    private fun unsupportedVersionMessage(
        diagnostic: ParserDiagnostic.UnsupportedSpecificationVersion,
    ): String {
        val reason =
            if (diagnostic.knownVersionLine) {
                "is recognized, but its parser profile is not implemented"
            } else {
                "is not supported"
            }
        return "AsyncAPI specification version '${diagnostic.declaredVersion}' $reason. " +
            "Supported version lines: ${diagnostic.supportedVersionLines.joinToString()}."
    }

    private fun unexpectedValueMessage(
        diagnostic: ParserDiagnostic.UnexpectedValueType,
    ): String {
        val actualDescription = diagnostic.actualValue?.let { " ${formatValue(it)}" }.orEmpty()
        val hint = scalarHint(diagnostic.expectedType, diagnostic.actualValue)
        return buildString {
            append(
                "Unexpected value: expected ${diagnostic.expectedType}, " +
                    "found ${diagnostic.actualType.displayName}$actualDescription.",
            )
            if (hint != null) {
                appendLine()
                append(hint)
            }
        }
    }

    private fun buildMessage(
        header: String,
        diagnostic: ParserDiagnostic,
        context: AsyncApiContext,
    ): String {
        val snippet = context.sourceSnippet(
            diagnostic.sourceLocation.copy(path = diagnostic.path),
        )
        return buildString {
            appendLine(header)
            appendLine()
            appendLine(
                snippet.ifBlank {
                    "→ ${diagnostic.sourceLocation.file.name} (${diagnostic.path})"
                },
            )
        }.trimEnd()
    }

    private fun scalarHint(
        expectedType: String,
        actualValue: Any?,
    ): String? {
        val expected = expectedType.lowercase().removeSuffix("?")
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

    private fun String.isNumberText(): Boolean = toDoubleOrNull() != null
}
