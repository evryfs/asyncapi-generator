package dev.banking.asyncapi.generator.core.source

import com.fasterxml.jackson.core.io.JsonStringEncoder

/**
 * Position of a value inside one document.
 *
 * A path starts at `root` and continues with object member names and zero-based array indexes, as in
 * `root.channels.userSignedUp` or `root.servers[0]`. A member name that contains a path delimiter is
 * JSON-quoted in brackets, as in `root["channel.name"]`, so two different paths never render the same.
 *
 * @param parent path of the enclosing value, or `null` for the root
 * @param rendered textual form of this path
 */
internal class NodePath private constructor(
    val parent: NodePath?,
    private val rendered: String,
) {
    /** Path of the member [name] of the object at this path. */
    fun member(name: String): NodePath = NodePath(this, appendMember(rendered, name))

    /** Path of the element at [index] of the array at this path. */
    fun index(index: Int): NodePath {
        require(index >= 0) { "Document array indexes must be nonnegative" }
        return NodePath(this, "$rendered[$index]")
    }

    /** This path followed by its parents, ending at the root. */
    fun ancestors(): Sequence<NodePath> = generateSequence(this, NodePath::parent)

    override fun equals(other: Any?): Boolean = other is NodePath && other.rendered == rendered

    override fun hashCode(): Int = rendered.hashCode()

    override fun toString(): String = rendered

    companion object {
        /** Path of the document root. */
        val ROOT = NodePath(parent = null, rendered = "root")

        private fun appendMember(
            parent: String,
            name: String,
        ): String =
            if (name.isSimpleMember()) {
                "$parent.$name"
            } else {
                "$parent[\"${String(JsonStringEncoder.getInstance().quoteAsString(name))}\"]"
            }

        private fun String.isSimpleMember(): Boolean =
            isNotEmpty() &&
                none { character ->
                    character == '.' ||
                        character == '[' ||
                        character == ']' ||
                        character == '"' ||
                        character == '\\' ||
                        character.isISOControl()
                }
    }
}
