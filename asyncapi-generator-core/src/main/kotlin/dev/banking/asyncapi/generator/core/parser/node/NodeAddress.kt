package dev.banking.asyncapi.generator.core.parser.node

import com.fasterxml.jackson.core.io.JsonStringEncoder

/** Collision-safe identity of one document node within one registered source. */
internal data class NodeAddress(
    val sourceId: String,
    val segments: List<NodeAddressSegment>,
) {
    val displayPath: String
        get() = sourceId + renderSegments()

    val documentPath: String
        get() = renderSegments().removePrefix(".")

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

    private fun renderSegments(): String = buildString {
        segments.forEach { segment ->
            when (segment) {
                is NodeAddressSegment.Member -> appendMember(segment.name)
                is NodeAddressSegment.Index -> append('[').append(segment.index).append(']')
            }
        }
    }

    private fun StringBuilder.appendMember(name: String) {
        if (name.isSimpleDisplayMember()) {
            append('.').append(name)
        } else {
            append("[\"")
            append(JsonStringEncoder.getInstance().quoteAsString(name))
            append("\"]")
        }
    }

    private fun String.isSimpleDisplayMember(): Boolean =
        isNotEmpty() && none { character ->
            character == '.' ||
                character == '[' ||
                character == ']' ||
                character == '"' ||
                character == '\\' ||
                character.isISOControl()
        }

    companion object {
        fun root(sourceId: String): NodeAddress =
            NodeAddress(sourceId, listOf(NodeAddressSegment.Member("root")))
    }
}

/** Typed path segment used by [NodeAddress] identity. */
internal sealed interface NodeAddressSegment {
    data class Member(val name: String) : NodeAddressSegment

    data class Index(val index: Int) : NodeAddressSegment
}
