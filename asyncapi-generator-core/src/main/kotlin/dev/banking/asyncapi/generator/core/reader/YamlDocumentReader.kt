package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentBoolean
import dev.banking.asyncapi.generator.core.document.DocumentMember
import dev.banking.asyncapi.generator.core.document.DocumentNode
import dev.banking.asyncapi.generator.core.document.DocumentNull
import dev.banking.asyncapi.generator.core.document.DocumentNumber
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.document.DocumentString
import dev.banking.asyncapi.generator.core.document.InputDocument
import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.document.appendDocumentIndex
import dev.banking.asyncapi.generator.core.document.appendDocumentMember
import org.yaml.snakeyaml.DumperOptions.ScalarStyle.PLAIN
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.Mark
import org.yaml.snakeyaml.error.MarkedYAMLException
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeId
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.SequenceNode
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.reader.ReaderException

/**
 * Reads YAML input into an [InputDocument].
 *
 * YAML presentation details such as quote style and block-scalar style must not
 * leak into semantic values. Source locations remain attached to the immutable
 * document nodes produced from the YAML representation graph.
 */
internal class YamlDocumentReader(
    private val limits: DocumentReaderLimits = DocumentReaderLimits.DEFAULT,
) {

    private val yaml =
        Yaml(
            LoaderOptions().apply {
                isAllowDuplicateKeys = false
                maxAliasesForCollections = limits.maxAliasesForCollections
                nestingDepthLimit = limits.maxNestingDepth
                codePointLimit = limits.maxDocumentCharacters
            },
        )

    fun read(source: DocumentSource): InputDocument {
        limits.requireDocumentSize(source)
        val content = source.content.removePrefix(UTF_8_BOM)
        if (content.isBlank()) {
            throw DocumentReadException.EmptyDocument(source.file)
        }

        val rootNode =
            try {
                yaml.compose(content.reader())
            } catch (ex: MarkedYAMLException) {
                throw DocumentReadException.MalformedDocument(
                    file = source.file,
                    cause = ex,
                    location = ex.problemMark?.let { locationOf(source, ROOT_PATH, it) },
                )
            } catch (ex: ReaderException) {
                throw DocumentReadException.MalformedDocument(
                    file = source.file,
                    cause = ex,
                    location = locationAtOffset(source, content, ex.position),
                )
            } catch (ex: YAMLException) {
                throw normalizeYamlException(source, ex)
            } ?: throw DocumentReadException.EmptyDocument(source.file)

        val root = parseNode(
            node = rootNode,
            path = ROOT_PATH,
            source = source,
        )

        return InputDocument(
            source = source,
            root = root,
        )
    }

    private fun parseNode(
        node: Node,
        path: String,
        source: DocumentSource,
    ): DocumentNode {
        val location = locationOf(source, path, node.startMark)
        return when (node.nodeId) {
            NodeId.scalar -> parseScalar(node as ScalarNode, location)
            NodeId.sequence -> parseSequence(node as SequenceNode, path, source, location)
            NodeId.mapping -> parseMapping(node as MappingNode, path, source, location)
            else -> throw malformed(
                location = location,
                message = "Unsupported YAML node kind '${node.nodeId}'",
            )
        }
    }

    private fun parseSequence(
        node: SequenceNode,
        path: String,
        source: DocumentSource,
        location: SourceLocation,
    ): DocumentArray {
        requireYamlTag(node, Tag.SEQ, location)
        return DocumentArray(
            elements = node.value.mapIndexed { index, child ->
                parseNode(child, appendDocumentIndex(path, index), source)
            },
            location = location,
        )
    }

    private fun parseMapping(
        node: MappingNode,
        path: String,
        source: DocumentSource,
        location: SourceLocation,
    ): DocumentObject {
        requireYamlTag(node, Tag.MAP, location)
        val result = linkedMapOf<String, DocumentMember>()
        node.value.forEach { tuple ->
            val keyNode = tuple.keyNode as? ScalarNode
                ?: throw invalidMappingKey(source, path, tuple.keyNode.startMark)
            if (keyNode.tag != Tag.STR) {
                throw invalidMappingKey(source, appendDocumentMember(path, keyNode.value), keyNode.startMark)
            }
            val key = keyNode.value
            val keyPath = appendDocumentMember(path, key)
            val keyLocation = locationOf(source, keyPath, keyNode.startMark)
            if (result.containsKey(key)) {
                throw DocumentReadException.DuplicateKey(source.file, key, keyLocation)
            }
            result[key] = DocumentMember(
                keyLocation = keyLocation,
                value = parseNode(tuple.valueNode, keyPath, source),
            )
        }
        return DocumentObject(result, location)
    }

    private fun parseScalar(
        node: ScalarNode,
        location: SourceLocation,
    ): DocumentNode =
        when (node.tag) {
            Tag.NULL -> DocumentNull(location)
            Tag.BOOL -> parseBoolean(node.value, location)
            Tag.INT -> parseNumber(node.value, location, DocumentNumberParser::parseInteger)
            Tag.FLOAT -> parseNumber(node.value, location, DocumentNumberParser::parseDecimal)
            Tag.STR -> parseString(node, location)
            Tag.TIMESTAMP -> DocumentString(node.value, location)
            else -> throw malformed(
                location = location,
                message = "Unsupported YAML scalar tag '${node.tag.value}'",
            )
        }

    private fun parseString(
        node: ScalarNode,
        location: SourceLocation,
    ): DocumentNode {
        if (node.scalarStyle == PLAIN && YAML_ONLY_PLAIN_NUMBER.matches(node.value)) {
            throw malformed(
                location = location,
                message = "YAML numeric value '${node.value}' is not a JSON-compatible number",
            )
        }
        return DocumentString(node.value, location)
    }

    private fun parseBoolean(
        value: String,
        location: SourceLocation,
    ): DocumentNode =
        when (value.lowercase()) {
            "true" -> DocumentBoolean(true, location)
            "false" -> DocumentBoolean(false, location)
            else -> DocumentString(value, location)
        }

    private fun parseNumber(
        value: String,
        location: SourceLocation,
        parse: (String) -> Number?,
    ): DocumentNode {
        limits.requireNumberLength(value, location)
        if (!JSON_NUMBER.matches(value)) {
            throw malformed(
                location = location,
                message = "YAML numeric value '$value' is not a JSON-compatible number",
            )
        }
        return parse(value)?.let { DocumentNumber(it, location) }
            ?: throw malformed(location, "Invalid numeric value '$value'")
    }

    private fun requireYamlTag(
        node: Node,
        expected: Tag,
        location: SourceLocation,
    ) {
        if (node.tag != expected) {
            throw malformed(
                location = location,
                message = "Unsupported YAML tag '${node.tag.value}' for ${node.nodeId} node",
            )
        }
    }

    private fun normalizeYamlException(
        source: DocumentSource,
        exception: YAMLException,
    ): DocumentReadException {
        val message = exception.message.orEmpty()
        val (limit, maximum) =
            when {
                message.startsWith("Nesting Depth exceeded max") ->
                    DocumentResourceLimit.NESTING_DEPTH to limits.maxNestingDepth
                message.startsWith("Number of aliases for non-scalar nodes exceeds the specified max=") ->
                    DocumentResourceLimit.COLLECTION_ALIASES to limits.maxAliasesForCollections
                message.startsWith("The incoming YAML document exceeds the limit:") ->
                    DocumentResourceLimit.DOCUMENT_CHARACTERS to limits.maxDocumentCharacters
                else -> return DocumentReadException.MalformedDocument(source.file, exception)
            }
        return DocumentReadException.ResourceLimitExceeded(
            file = source.file,
            limit = limit,
            maximum = maximum.toLong(),
            cause = exception,
        )
    }

    private fun locationAtOffset(
        source: DocumentSource,
        content: String,
        offset: Int,
    ): SourceLocation {
        val boundedOffset = offset.coerceIn(0, content.length)
        val prefix = content.substring(0, boundedOffset)
        val line = prefix.count { it == '\n' } + 1
        val lastLineBreak = prefix.lastIndexOf('\n')
        val column = boundedOffset - lastLineBreak
        return SourceLocation.from(source, ROOT_PATH, line, column)
    }

    private fun malformed(
        location: SourceLocation,
        message: String,
    ): DocumentReadException.MalformedDocument =
        DocumentReadException.MalformedDocument(
            file = location.file,
            cause = IllegalArgumentException(message),
            location = location,
        )

    private fun locationOf(
        source: DocumentSource,
        path: String,
        mark: Mark,
    ): SourceLocation =
        SourceLocation.from(
            source = source,
            path = path,
            line = mark.line + 1,
            column = mark.column + 1,
        )

    private fun invalidMappingKey(
        source: DocumentSource,
        path: String,
        mark: Mark,
    ): DocumentReadException.InvalidMappingKey =
        DocumentReadException.InvalidMappingKey(
            file = source.file,
            location = locationOf(source, path, mark),
        )

    private companion object {
        const val ROOT_PATH = "root"
        const val UTF_8_BOM = "\uFEFF"
        val JSON_NUMBER = Regex("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")
        val YAML_ONLY_PLAIN_NUMBER = Regex("[-+]?0[oO][0-7](?:_?[0-7])*")
    }
}
