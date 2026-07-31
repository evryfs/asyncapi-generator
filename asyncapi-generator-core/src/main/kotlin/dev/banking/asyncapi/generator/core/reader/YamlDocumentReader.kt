package dev.banking.asyncapi.generator.core.reader

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.Mark
import org.yaml.snakeyaml.error.MarkedYAMLException
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeId
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.SequenceNode
import org.yaml.snakeyaml.nodes.Tag
import java.io.File

/**
 * Reads YAML input into an [InputDocument].
 *
 * YAML presentation details such as quote style and block-scalar style must not
 * leak into semantic values. Source locations are recorded for document paths
 * that can be mapped from the YAML node tree.
 *
 * Expected behavior is covered by:
 * - `YamlDocumentReaderTest`
 * - `DocumentReaderContractTest`
 * - `DocumentLocationTest`
 */
class YamlDocumentReader : DocumentReader {
    private val yaml =
        Yaml(
            LoaderOptions().apply {
                isProcessComments = true
                isAllowDuplicateKeys = false
            },
        )

    override fun read(source: DocumentSource): InputDocument {
        if (source.content.isBlank()) {
            throw DocumentReadException.EmptyDocument(source.file)
        }

        val rootNode =
            try {
                yaml.compose(source.content.reader())
            } catch (ex: MarkedYAMLException) {
                throw DocumentReadException.MalformedDocument(source.file, ex)
            } ?: throw DocumentReadException.EmptyDocument(source.file)

        val rootNodeValue = parseNode(
            node = rootNode,
            path = ROOT_PATH,
            source = source,
        )
        val root = rootNodeValue as? DocumentObject
            ?: throw DocumentReadException.InvalidRoot(source.file, typeName(rootNodeValue))

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
        val members = linkedMapOf<String, DocumentMember>()
        node.value.forEach { tuple ->
            val keyNode = tuple.keyNode as? ScalarNode
                ?: throw invalidMappingKey(source.file, tuple.keyNode.startMark)
            val key = keyNode.value
            val keyLocation = locationOf(source, "$path.$key", keyNode.startMark)
            if (members.containsKey(key)) {
                throw DocumentReadException.DuplicateKey(source.file, key, keyLocation.line, keyLocation.column)
            }
            val keyPath = "$path.$key"
            members[key] = DocumentMember(
                keyLocation = keyLocation,
                value = parseNode(tuple.valueNode, keyPath, source),
            )
        }
        return DocumentObject(members, location)
    }

    private fun parseScalar(
        node: ScalarNode,
        location: SourceLocation,
    ): DocumentNode =
        when (node.tag) {
            Tag.NULL -> DocumentNull(location)
            Tag.BOOL -> parseBoolean(node.value, location)
            Tag.INT -> parseInteger(node.value)?.let { DocumentNumber(it, location) }
                ?: DocumentString(node.value, location)
            Tag.FLOAT -> parseFloat(node.value)?.let { DocumentNumber(it, location) }
                ?: DocumentString(node.value, location)
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

    private fun parseInteger(value: String): Number? {
        val normalized = value.replace("_", "")
        return normalized.toLongOrNull()?.let {
            if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it
        }
    }

    private fun parseFloat(value: String): Double? =
        value.replace("_", "").toDoubleOrNull()

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
