package dev.banking.asyncapi.generator.core.document

import java.util.Collections

/**
 * A format-independent value read from an input document.
 *
 * This sealed hierarchy is the value-tree contract between document readers
 * and parsers. It represents only document structure and scalar values; it
 * does not assign AsyncAPI meaning to them. Every node retains the location at
 * which its value begins in the original source.
 *
 * Object members additionally retain the location of their key so diagnostics
 * can distinguish a member declaration from its value.
 *
 * @property location source file, parser path, line, and column for this value
 */
sealed interface DocumentNode {
    val location: SourceLocation
}

/**
 * An object value with source-located members in document order.
 *
 * The supplied [members] are defensively copied and exposed through an
 * unmodifiable map, making the object an immutable snapshot of reader output.
 *
 * @param members members keyed by their document field names
 * @property location location at which the object value begins
 */
class DocumentObject(
    members: Map<String, DocumentMember>,
    override val location: SourceLocation,
) : DocumentNode {
    /** Members in the same order in which their keys appeared in the source. */
    val members: Map<String, DocumentMember> =
        Collections.unmodifiableMap(LinkedHashMap(members))

    /**
     * Returns the value of [name], or `null` when the member is absent.
     *
     * An explicitly declared YAML or JSON null is returned as [DocumentNull],
     * so absence and explicit null remain distinguishable.
     */
    operator fun get(name: String): DocumentNode? = members[name]?.value

    /** Returns the complete member, including its key location, when present. */
    fun member(name: String): DocumentMember? = members[name]
}

/**
 * An object member that retains the locations of both its key and value.
 *
 * @property keyLocation location at which the member name begins
 * @property value source-located value assigned to the member
 */
data class DocumentMember(
    val keyLocation: SourceLocation,
    val value: DocumentNode,
)

/**
 * An ordered array value.
 *
 * The supplied [elements] are defensively copied and exposed through an
 * unmodifiable list, making the array an immutable snapshot of reader output.
 *
 * @param elements values in document order
 * @property location location at which the array value begins
 */
class DocumentArray(
    elements: List<DocumentNode>,
    override val location: SourceLocation,
) : DocumentNode {
    /** Elements in the same order in which they appeared in the source. */
    val elements: List<DocumentNode> =
        Collections.unmodifiableList(ArrayList(elements))

    /** Returns the element at [index]. */
    operator fun get(index: Int): DocumentNode = elements[index]
}

/** A string value and its source [location]. */
data class DocumentString(
    val value: String,
    override val location: SourceLocation,
) : DocumentNode

/**
 * A numeric value and its source [location].
 *
 * Equivalent YAML and JSON numbers use the same runtime [Number]
 * representation so parsers do not depend on reader-specific number types.
 */
data class DocumentNumber(
    val value: Number,
    override val location: SourceLocation,
) : DocumentNode

/** A boolean value and its source [location]. */
data class DocumentBoolean(
    val value: Boolean,
    override val location: SourceLocation,
) : DocumentNode

/**
 * An explicit YAML or JSON null value.
 *
 * This node is present in the document tree and is therefore distinct from an
 * absent object member or unavailable array element.
 */
data class DocumentNull(
    override val location: SourceLocation,
) : DocumentNode

/**
 * Converts this node to maps, lists, scalar values, and Kotlin `null`.
 *
 * Object member order is preserved, but source and key-location metadata is
 * intentionally discarded.
 */
internal fun DocumentNode.toValue(): Any? =
    when (this) {
        is DocumentObject -> members.mapValuesTo(linkedMapOf()) { (_, member) ->
            member.value.toValue()
        }

        is DocumentArray -> elements.map(DocumentNode::toValue)
        is DocumentString -> value
        is DocumentNumber -> value
        is DocumentBoolean -> value
        is DocumentNull -> null
    }
