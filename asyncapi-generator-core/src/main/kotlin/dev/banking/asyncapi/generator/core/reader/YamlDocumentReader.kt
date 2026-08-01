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
import java.io.File

/**
 * Reads YAML input into an [InputDocument].
 *
 * YAML presentation details such as quote style and block-scalar style must not
 * leak into semantic values. Source locations remain attached to the immutable
 * document nodes produced from the YAML representation graph.
 *
 * Expected behavior is covered by:
 * - `YamlDocumentReaderTest`
 * - `DocumentReaderContractTest`
 * - `DocumentLocationTest`
 */
class YamlDocumentReader internal constructor(
    private val limits: DocumentReaderLimits,
) : DocumentReader {
    constructor() : this(DocumentReaderLimits.DEFAULT)

    private val yaml =
        Yaml(
            LoaderOptions().apply {
                isProcessComments = true
                isAllowDuplicateKeys = false
                maxAliasesForCollections = limits.maxAliasesForCollections
                nestingDepthLimit = limits.maxNestingDepth
                codePointLimit = limits.maxDocumentCharacters
            },
        )

    override fun read(source: DocumentSource): InputDocument {
        limits.requireDocumentSize(source)
        if (source.content.isBlank()) {
            throw DocumentReadException.EmptyDocument(source.file)
        }

        val rootNode =
            try {
                yaml.compose(source.content.reader())
            } catch (ex: MarkedYAMLException) {
                throw DocumentReadException.MalformedDocument(source.file, ex)
            } catch (ex: ReaderException) {
                throw DocumentReadException.MalformedDocument(source.file, ex)
            } catch (ex: YAMLException) {
                throw DocumentReadException.ResourceLimitExceeded(source.file, ex)
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
            else -> DocumentNull(location)
        }
    }

    private fun parseSequence(
        node: SequenceNode,
        path: String,
        source: DocumentSource,
        location: SourceLocation,
    ): DocumentArray =
        DocumentArray(
            elements = node.value.mapIndexed { index, child ->
                parseNode(child, "$path[$index]", source)
            },
            location = location,
        )

    private fun parseMapping(
        node: MappingNode,
        path: String,
        source: DocumentSource,
        location: SourceLocation,
    ): DocumentObject {
        val result = linkedMapOf<String, DocumentMember>()
        node.value.forEach { tuple ->
            val keyNode = tuple.keyNode as? ScalarNode
                ?: throw invalidMappingKey(source.file, tuple.keyNode.startMark)
            if (keyNode.tag != Tag.STR) {
                throw invalidMappingKey(source.file, keyNode.startMark)
            }
            val key = keyNode.value
            val keyLocation = locationOf(source, "$path.$key", keyNode.startMark)
            if (result.containsKey(key)) {
                throw DocumentReadException.DuplicateKey(source.file, key, keyLocation.line, keyLocation.column)
            }
            val keyPath = "$path.$key"
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
            else -> DocumentString(node.value, location)
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
        limits.requireNumberLength(location.file, value)
        return parse(value)?.let { DocumentNumber(it, location) }
            ?: DocumentString(value, location)
    }

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
        file: File,
        mark: Mark,
    ): DocumentReadException.InvalidMappingKey =
        DocumentReadException.InvalidMappingKey(
            file = file,
            line = mark.line + 1,
            column = mark.column + 1,
        )

    private companion object {
        const val ROOT_PATH = "root"
    }
}
