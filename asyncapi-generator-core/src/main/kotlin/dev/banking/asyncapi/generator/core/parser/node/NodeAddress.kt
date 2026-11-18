package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.source.NodePath
import dev.banking.asyncapi.generator.core.source.SourceId

/**
 * Identity of one document node across all sources of a load.
 *
 * @property sourceId source document the node belongs to
 * @property path position of the node within that document
 */
internal data class NodeAddress(
    val sourceId: SourceId,
    val path: NodePath,
) {
    /** The path prefixed with the source id, unique across all sources of a load. */
    val displayPath: String
        get() = if (sourceId.value.isEmpty()) path.toString() else "$sourceId.$path"

    /** The path within the source document, without the source id. */
    val documentPath: String
        get() = path.toString()

    val parent: NodeAddress?
        get() = path.parent?.let { NodeAddress(sourceId, it) }

    fun member(name: String): NodeAddress = NodeAddress(sourceId, path.member(name))

    fun index(index: Int): NodeAddress = NodeAddress(sourceId, path.index(index))

    fun ancestors(): Sequence<NodeAddress> = generateSequence(this, NodeAddress::parent)

    companion object {
        fun root(sourceId: SourceId): NodeAddress = NodeAddress(sourceId, NodePath.ROOT)
    }
}
