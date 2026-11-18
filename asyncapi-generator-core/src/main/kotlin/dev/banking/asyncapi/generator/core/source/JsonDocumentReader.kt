package dev.banking.asyncapi.generator.core.source

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonFactoryBuilder
import com.fasterxml.jackson.core.JsonLocation
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.core.exc.StreamConstraintsException

/**
 * Reads JSON input into an [InputDocument].
 *
 * JSON input must produce the same document tree shape as equivalent YAML
 * input so later stages remain format-independent.
 *
 * @param limits resource limits enforced during parsing
 */
internal class JsonDocumentReader(
    override val limits: SourceLimits = SourceLimits(),
) : DocumentReader {
    private val jsonFactory: JsonFactory =
        JsonFactoryBuilder()
            .streamReadConstraints(
                StreamReadConstraints
                    .builder()
                    .maxDocumentLength(limits.maxDocumentCharacters.toLong())
                    .maxNestingDepth(limits.maxNestingDepth)
                    .maxNumberLength(limits.maxNumberCharacters)
                    .build(),
            ).build()

    override fun readRoot(
        source: DocumentSource,
        content: String,
    ): DocumentNode =
        try {
            jsonFactory.createParser(content).use { parser ->
                val rootToken =
                    parser.nextToken()
                        ?: throw DocumentReadException.EmptyDocument(source.file)
                val root = parseNode(parser, rootToken, NodePath.ROOT, source)
                val trailingToken = parser.nextToken()
                if (trailingToken != null) {
                    throw malformed(source, parser, "Unexpected content after JSON root")
                }
                root
            }
        } catch (exception: StreamConstraintsException) {
            throw resourceLimitExceeded(source, exception)
        } catch (exception: JsonProcessingException) {
            throw DocumentReadException.MalformedDocument(
                file = source.file,
                cause = exception,
                location = exception.location?.let { locationOf(source, NodePath.ROOT, it) },
            )
        }

    private fun parseNode(
        parser: JsonParser,
        token: JsonToken,
        path: NodePath,
        source: DocumentSource,
    ): DocumentNode {
        val location = locationOf(source, path, parser.currentTokenLocation())
        return when (token) {
            JsonToken.START_OBJECT -> parseObject(parser, path, source, location)
            JsonToken.START_ARRAY -> parseArray(parser, path, source, location)
            JsonToken.VALUE_STRING -> DocumentString(parser.valueAsString, location)
            JsonToken.VALUE_TRUE -> DocumentBoolean(true, location)
            JsonToken.VALUE_FALSE -> DocumentBoolean(false, location)
            JsonToken.VALUE_NUMBER_INT ->
                parseNumber(parser, source, location, "Invalid JSON integer", DocumentNumberParser::parseInteger)
            JsonToken.VALUE_NUMBER_FLOAT ->
                parseNumber(parser, source, location, "Invalid JSON decimal", DocumentNumberParser::parseDecimal)
            JsonToken.VALUE_NULL -> DocumentNull(location)
            else -> throw malformed(source, parser, "Unexpected JSON token: $token")
        }
    }

    private fun parseNumber(
        parser: JsonParser,
        source: DocumentSource,
        location: SourceLocation,
        invalidMessage: String,
        parse: (String) -> Number?,
    ): DocumentNumber =
        DocumentNumber(
            value = parse(parser.text) ?: throw malformed(source, parser, invalidMessage),
            location = location,
        )

    private fun parseObject(
        parser: JsonParser,
        path: NodePath,
        source: DocumentSource,
        location: SourceLocation,
    ): DocumentObject {
        val members = DocumentObjectBuilder(source.file)
        while (true) {
            val token =
                parser.nextToken()
                    ?: throw malformed(source, parser, "Unexpected end of JSON object")
            if (token == JsonToken.END_OBJECT) {
                return members.build(location)
            }
            if (token != JsonToken.FIELD_NAME) {
                throw malformed(source, parser, "Expected JSON field name, found $token")
            }

            val key = parser.currentName()
            val keyPath = path.member(key)
            val keyLocation = locationOf(source, keyPath, parser.currentTokenLocation())
            members.add(key, keyLocation) { parseMemberValue(parser, key, keyPath, source) }
        }
    }

    private fun parseMemberValue(
        parser: JsonParser,
        key: String,
        path: NodePath,
        source: DocumentSource,
    ): DocumentNode {
        val valueToken =
            parser.nextToken()
                ?: throw malformed(source, parser, "Missing value for JSON field '$key'")
        return parseNode(parser, valueToken, path, source)
    }

    private fun parseArray(
        parser: JsonParser,
        path: NodePath,
        source: DocumentSource,
        location: SourceLocation,
    ): DocumentArray {
        val result = mutableListOf<DocumentNode>()
        while (true) {
            val token =
                parser.nextToken()
                    ?: throw malformed(source, parser, "Unexpected end of JSON array")
            if (token == JsonToken.END_ARRAY) {
                return DocumentArray(result, location)
            }
            result += parseNode(parser, token, path.index(result.size), source)
        }
    }

    private fun locationOf(
        source: DocumentSource,
        path: NodePath,
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
        DocumentReadException.MalformedDocument(
            file = source.file,
            cause = JsonParseException(parser, message),
            location = locationOf(source, NodePath.ROOT, parser.currentLocation()),
        )

    private fun resourceLimitExceeded(
        source: DocumentSource,
        exception: StreamConstraintsException,
    ): DocumentReadException.ResourceLimitExceeded {
        val message = exception.message.orEmpty().lowercase()
        val (limit, maximum) =
            when {
                "nesting depth" in message ->
                    DocumentResourceLimit.NESTING_DEPTH to limits.maxNestingDepth
                "number value length" in message || "number length" in message ->
                    DocumentResourceLimit.NUMBER_CHARACTERS to limits.maxNumberCharacters
                else ->
                    DocumentResourceLimit.DOCUMENT_CHARACTERS to limits.maxDocumentCharacters
            }
        return DocumentReadException.ResourceLimitExceeded(
            file = source.file,
            limit = limit,
            maximum = maximum.toLong(),
            cause = exception,
            location = exception.location?.let { locationOf(source, NodePath.ROOT, it) },
        )
    }
}
