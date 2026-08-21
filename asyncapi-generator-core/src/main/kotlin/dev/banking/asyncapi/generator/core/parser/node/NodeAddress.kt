package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.document.appendDocumentIndex
import dev.banking.asyncapi.generator.core.document.appendDocumentMember

/**
 * Collision-safe identity of one document node within one registered source.
 *
 * @property sourceId identifies the source document this node belongs to
 * @property segments ordered path segments from root to this node
 */
internal data class NodeAddress(
    val sourceId: String,
    val segments: List<NodeAddressSegment>,
) {
    val displayPath: String
        get() = renderSegments(sourceId)

    val documentPath: String
        get() = renderSegments("")

    val parent: NodeAddress?
        get() = segments.dropLast(1).takeIf(List<*>::isNotEmpty)?.let { NodeAddress(sourceId, it) }

    fun member(name: String): NodeAddress =
        NodeAddress(sourceId, segments + NodeAddressSegment.Member(name))

    fun index(index: Int): NodeAddress {
        require(index >= 0) { "Document array indexes must be nonnegative" }
        return NodeAddress(sourceId, segments + NodeAddressSegment.Index(index))
    }

    fun ancestors(): Sequence<NodeAddress> =
        generateSequence(this, NodeAddress::parent)

    private fun renderSegments(initialPath: String): String =
        segments.fold(initialPath) { path, segment ->
            when (segment) {
                is NodeAddressSegment.Member -> appendDocumentMember(path, segment.name)
                is NodeAddressSegment.Index -> appendDocumentIndex(path, segment.index)
            }
        }

    companion object {
        fun root(sourceId: String): NodeAddress =
            NodeAddress(sourceId, listOf(NodeAddressSegment.Member("root")))
    }
}

/** A single segment in a [NodeAddress] path — either a named member or a numeric index. */
internal sealed interface NodeAddressSegment {
    /** An object member identified by its key name. */
    data class Member(val name: String) : NodeAddressSegment

    /** An array element identified by its zero-based index. */
    data class Index(val index: Int) : NodeAddressSegment
}
