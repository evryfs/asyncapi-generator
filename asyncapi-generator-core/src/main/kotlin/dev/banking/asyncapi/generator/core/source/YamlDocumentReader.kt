package dev.banking.asyncapi.generator.core.source

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
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Reads YAML input into an [InputDocument].
 *
 * YAML presentation details such as quote style and block-scalar style must not
 * leak into semantic values. Source locations remain attached to the immutable
 * document nodes produced from the YAML representation graph.
 *
 * @param limits resource limits enforced during parsing
 */
internal class YamlDocumentReader(
    override val limits: SourceLimits = SourceLimits(),
) : DocumentReader {
    private val scalars = YamlScalarReader(limits)

    private val yaml =
        Yaml(
            LoaderOptions().apply {
                isAllowDuplicateKeys = false
                maxAliasesForCollections = limits.maxAliasesForCollections
                nestingDepthLimit = limits.maxNestingDepth
                codePointLimit = limits.maxDocumentCharacters
            },
        )

    override fun readRoot(
        source: DocumentSource,
        content: String,
    ): DocumentNode {
        val rootNode =
            try {
                yaml.compose(content.reader())
            } catch (exception: MarkedYAMLException) {
                throw DocumentReadException.MalformedDocument(
                    file = source.file,
                    cause = exception,
                    location = exception.problemMark?.let { locationOf(source, NodePath.ROOT, it) },
                )
            } catch (exception: ReaderException) {
                throw DocumentReadException.MalformedDocument(
                    file = source.file,
                    cause = exception,
                    location = locationAtOffset(source, content, exception.position),
                )
            } catch (exception: YAMLException) {
                throw normalizeYamlException(source, exception)
            } ?: throw DocumentReadException.EmptyDocument(source.file)

        return parseNode(
            node = rootNode,
            path = NodePath.ROOT,
            source = source,
        )
    }

    /**
     * @param node the YAML node to convert
     * @param path path of [node] in the document
     * @param source the document being read
     * @param ancestors YAML nodes on the path from the root to [node]. An alias can point at one of
     *   its own ancestors, which makes the YAML graph infinite; that is rejected here.
     */
    private fun parseNode(
        node: Node,
        path: NodePath,
        source: DocumentSource,
        ancestors: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap()),
    ): DocumentNode {
        val location = locationOf(source, path, node.startMark)
        if (!ancestors.add(node)) {
            throw DocumentReadException.MalformedDocument.at(
                location = location,
                reason = "Recursive YAML alias at $path: the aliased node contains itself",
            )
        }
        try {
            return when (node.nodeId) {
                NodeId.scalar -> scalars.read(node as ScalarNode, location)
                NodeId.sequence -> parseSequence(node as SequenceNode, path, source, location, ancestors)
                NodeId.mapping -> parseMapping(node as MappingNode, path, source, location, ancestors)
                else -> throw DocumentReadException.MalformedDocument.at(
                    location = location,
                    reason = "Unsupported YAML node kind '${node.nodeId}'",
                )
            }
        } finally {
            ancestors.remove(node)
        }
    }

    private fun parseSequence(
        node: SequenceNode,
        path: NodePath,
        source: DocumentSource,
        location: SourceLocation,
        ancestors: MutableSet<Node>,
    ): DocumentArray {
        requireYamlTag(node, Tag.SEQ, location)
        return DocumentArray(
            elements =
                node.value.mapIndexed { index, child ->
                    parseNode(child, path.index(index), source, ancestors)
                },
            location = location,
        )
    }

    private fun parseMapping(
        node: MappingNode,
        path: NodePath,
        source: DocumentSource,
        location: SourceLocation,
        ancestors: MutableSet<Node>,
    ): DocumentObject {
        requireYamlTag(node, Tag.MAP, location)
        val members = DocumentObjectBuilder(source.file)
        node.value.forEach { tuple ->
            val keyNode =
                tuple.keyNode as? ScalarNode
                    ?: throw invalidMappingKey(source, path, tuple.keyNode.startMark)
            if (keyNode.tag != Tag.STR) {
                throw invalidMappingKey(source, path.member(keyNode.value), keyNode.startMark)
            }
            val key = keyNode.value
            val keyPath = path.member(key)
            val keyLocation = locationOf(source, keyPath, keyNode.startMark)
            members.add(key, keyLocation) { parseNode(tuple.valueNode, keyPath, source, ancestors) }
        }
        return members.build(location)
    }

    private fun requireYamlTag(
        node: Node,
        expected: Tag,
        location: SourceLocation,
    ) {
        if (node.tag != expected) {
            throw DocumentReadException.MalformedDocument.at(
                location = location,
                reason = "Unsupported YAML tag '${node.tag.value}' for ${node.nodeId} node",
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
        return SourceLocation.from(source, NodePath.ROOT, line, column)
    }

    private fun locationOf(
        source: DocumentSource,
        path: NodePath,
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
        path: NodePath,
        mark: Mark,
    ): DocumentReadException.InvalidMappingKey =
        DocumentReadException.InvalidMappingKey(
            file = source.file,
            location = locationOf(source, path, mark),
        )
}
