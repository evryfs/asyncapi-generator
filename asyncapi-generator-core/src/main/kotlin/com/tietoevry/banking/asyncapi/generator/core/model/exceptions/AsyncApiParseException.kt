package com.tietoevry.banking.asyncapi.generator.core.model.exceptions

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext

sealed class AsyncApiParseException(message: String) : Exception(message) {

    class EmptyYamlFile(fileName: String) :
        AsyncApiParseException("Empty Yaml file : $fileName")

    class Mandatory(name: String, path: String, context: AsyncApiContext) :
        AsyncApiParseException(buildMessage("Missing mandatory '$name'", path, context))

    class UnsupportedSchemaFormat(format: String, path: String, context: AsyncApiContext) :
        AsyncApiParseException(buildMessage("SchemaFormat: $format is not supported.", path, context))

    class UnexpectedSchemaFormat(format: String, path: String, context: AsyncApiContext) :
        AsyncApiParseException(buildMessage("SchemaFormat: $format is not valid.", path, context))

    class UnexpectedValue(receivedValue: String, expectedValue: String, path: String, context: AsyncApiContext) :
        AsyncApiParseException(
            buildMessage(
                "Unexpected value: $receivedValue ${if (expectedValue.isNotEmpty()) ", expected: $expectedValue" else ""}",
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
    }
}

