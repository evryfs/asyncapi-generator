package dev.banking.asyncapi.generator.core.parser.diagnostics

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.tracking.SourceTracking

/** The failure for [diagnostic], with its message and a snippet of the source it points at. */
internal fun AsyncApiContext.parserFailure(
    diagnostic: ParserDiagnostic,
): AsyncApiParseException.ParserDiagnosticFailure =
    AsyncApiParseException.ParserDiagnosticFailure(
        diagnostic,
        ParserDiagnosticFormatter.format(diagnostic, sourceTracking),
    )

/** Renders parser diagnostics as a one-line header followed by a snippet of the source. */
internal object ParserDiagnosticFormatter {
    /** The message of [diagnostic] with a snippet from [sources]. */
    fun format(
        diagnostic: ParserDiagnostic,
        sources: SourceTracking,
    ): String {
        val snippet = sources.snippet(diagnostic.sourceLocation, label = diagnostic.path)
        return buildString {
            appendLine(header(diagnostic))
            appendLine()
            appendLine(snippet.ifBlank { "→ ${diagnostic.sourceLocation.file.name} (${diagnostic.path})" })
        }.trimEnd()
    }

    private fun header(diagnostic: ParserDiagnostic): String =
        when (diagnostic) {
            is ParserDiagnostic.MissingRequiredMember -> "Missing required member '${diagnostic.memberName}'."
            is ParserDiagnostic.UnexpectedValueType -> unexpectedValueMessage(diagnostic)
            is ParserDiagnostic.UnexpectedObjectMember ->
                "Unexpected member '${diagnostic.memberName}': expected ${diagnostic.expectedMember}."
            is ParserDiagnostic.InvalidSpecificationVersion ->
                "Invalid AsyncAPI specification version '${diagnostic.declaredVersion}': " +
                    "expected major.minor.patch with an optional alphanumeric suffix."
            is ParserDiagnostic.UnsupportedSpecificationVersion -> unsupportedVersionMessage(diagnostic)
            is ParserDiagnostic.InvalidReference -> "Invalid reference '${diagnostic.reference}': ${diagnostic.reason}."
            is ParserDiagnostic.ReferenceDocumentNotFound ->
                "Reference document for '${diagnostic.reference}' was not found or is not readable: " +
                    diagnostic.resolvedFile
            is ParserDiagnostic.ReferenceTargetNotFound -> "Reference target '${diagnostic.reference}' was not found."
            is ParserDiagnostic.LoadResourceLimitExceeded ->
                "Parser load exceeded the ${diagnostic.limit.displayName} limit of " +
                    "${diagnostic.maximum}: observed ${diagnostic.observed}."
            is ParserDiagnostic.UnknownSchemaFormat -> "SchemaFormat: ${diagnostic.schemaFormat} is not valid."
            is ParserDiagnostic.NativeSchemaAssetUnreadable ->
                "Native schema asset '${diagnostic.reference}' could not be read. Reason: ${diagnostic.reason}"
        }

    private fun unsupportedVersionMessage(diagnostic: ParserDiagnostic.UnsupportedSpecificationVersion): String {
        val reason =
            if (diagnostic.knownVersionLine) {
                "is recognized, but its parser profile is not implemented"
            } else {
                "is not supported"
            }
        return "AsyncAPI specification version '${diagnostic.declaredVersion}' $reason. " +
            "Supported version lines: ${diagnostic.supportedVersionLines.joinToString()}."
    }

    private fun unexpectedValueMessage(diagnostic: ParserDiagnostic.UnexpectedValueType): String {
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

    private fun String.isYaml11BooleanWord(): Boolean = lowercase() in setOf("yes", "no", "on", "off")

    private fun String.isNumberText(): Boolean = toDoubleOrNull() != null
}
