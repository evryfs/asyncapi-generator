package dev.banking.asyncapi.generator.core.reader

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonLocation
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken

/**
 * Reads JSON input into an [InputDocument].
 *
 * JSON input must produce the same document tree shape as equivalent YAML
 * input so later stages remain format-independent.
 *
 * Expected behavior is covered by:
 * - `JsonDocumentReaderTest`
 * - `DocumentReaderContractTest`
 * - `DocumentLocationTest`
 */
class JsonDocumentReader : DocumentReader {
    private val jsonFactory = JsonFactory()

    override fun read(source: DocumentSource): InputDocument {
        if (source.content.isBlank()) {
            throw DocumentReadException.EmptyDocument(source.file)
        }

        return try {
            jsonFactory.createParser(source.content).use { parser ->
                val rootToken = parser.nextToken()
                    ?: throw DocumentReadException.EmptyDocument(source.file)
                val rootValue = parseNode(parser, rootToken, ROOT_PATH, source)
                val root = rootValue as? DocumentObject
                    ?: throw DocumentReadException.InvalidRoot(source.file, typeName(rootValue))
                val trailingToken = parser.nextToken()
                if (trailingToken != null) {
                    throw malformed(source, parser, "Unexpected content after JSON root")
                }
                InputDocument(
                    source = source,
                    root = root,
                )
            }
        } catch (ex: DocumentReadException) {
            throw ex
        } catch (ex: JsonParseException) {
            throw DocumentReadException.MalformedDocument(source.file, ex)
        }
    }

    private fun parseNode(
        parser: JsonParser,
        token: JsonToken,
        path: String,
        source: DocumentSource,
    ): DocumentNode {
        val location = locationOf(source, path, parser.currentTokenLocation())
        return when (token) {
            JsonToken.START_OBJECT -> parseObject(parser, path, source, location)
            JsonToken.START_ARRAY -> parseArray(parser, path, source, location)
            JsonToken.VALUE_STRING -> DocumentString(parser.valueAsString, location)
            JsonToken.VALUE_TRUE -> DocumentBoolean(true, location)
            JsonToken.VALUE_FALSE -> DocumentBoolean(false, location)
            JsonToken.VALUE_NUMBER_INT -> DocumentNumber(parser.numberValue, location)
            JsonToken.VALUE_NUMBER_FLOAT -> DocumentNumber(parser.doubleValue, location)
            JsonToken.VALUE_NULL -> DocumentNull(location)
            else -> throw malformed(source, parser, "Unexpected JSON token: $token")
        }
    }

    private fun parseObject(
        parser: JsonParser,
        path: String,
        source: DocumentSource,
        location: SourceLocation,
    ): DocumentObject {
        val result = linkedMapOf<String, DocumentMember>()
        while (true) {
            val token = parser.nextToken()
                ?: throw malformed(source, parser, "Unexpected end of JSON object")
            if (token == JsonToken.END_OBJECT) {
                return DocumentObject(result, location)
            }
            if (token != JsonToken.FIELD_NAME) {
                throw malformed(source, parser, "Expected JSON field name, found $token")
            }

            val key = parser.currentName()
            val keyPath = "$path.$key"
            val keyLocation = locationOf(source, keyPath, parser.currentTokenLocation())
            if (result.containsKey(key)) {
                throw DocumentReadException.DuplicateKey(
                    file = source.file,
                    key = key,
                    line = keyLocation.line,
                    column = keyLocation.column,
                )
            }

            val valueToken = parser.nextToken()
                ?: throw malformed(source, parser, "Missing value for JSON field '$key'")
            result[key] = DocumentMember(
                keyLocation = keyLocation,
                value = parseNode(parser, valueToken, keyPath, source),
            )
        }
    }

    private fun parseArray(
        parser: JsonParser,
        path: String,
        source: DocumentSource,
        location: SourceLocation,
    ): DocumentArray {
        val result = mutableListOf<DocumentNode>()
        while (true) {
            val token = parser.nextToken()
                ?: throw malformed(source, parser, "Unexpected end of JSON array")
            if (token == JsonToken.END_ARRAY) {
                return DocumentArray(result, location)
            }
            result += parseNode(parser, token, "$path[${result.size}]", source)
        }
    }

    private fun locationOf(
        source: DocumentSource,
        path: String,
        location: JsonLocation,
    ): SourceLocation =
        SourceLocation.from(
            source = source,
            path = path,
            line = location.lineNr.coerceAtLeast(1),
            column = location.columnNr.coerceAtLeast(1),
        )

    private fun malformed(
        source: DocumentSource,
        parser: JsonParser,
        message: String,
    ): DocumentReadException.MalformedDocument =
        DocumentReadException.MalformedDocument(source.file, JsonParseException(parser, message))

    private fun typeName(value: DocumentNode): String =
        when (value) {
            is DocumentObject -> "object"
            is DocumentArray -> "array"
            is DocumentString -> "string"
            is DocumentNumber -> "number"
            is DocumentBoolean -> "boolean"
            is DocumentNull -> "null"
        }

    private companion object {
        const val ROOT_PATH = "root"
    }
}
