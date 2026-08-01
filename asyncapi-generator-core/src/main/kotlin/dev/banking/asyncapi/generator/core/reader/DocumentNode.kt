package dev.banking.asyncapi.generator.core.reader

import java.util.Collections

/**
 * Immutable, format-independent value read from an input document.
 *
 * Every value retains its source location. Object members additionally retain
 * the location of their key so parser diagnostics and model metadata can point
 * to the member that introduced a value.
 */
sealed interface DocumentNode {
    val location: SourceLocation
}

class DocumentObject(
    members: Map<String, DocumentMember>,
    override val location: SourceLocation,
) : DocumentNode {
    val members: Map<String, DocumentMember> =
        Collections.unmodifiableMap(LinkedHashMap(members))

    operator fun get(name: String): DocumentNode? = members[name]?.value

    fun member(name: String): DocumentMember? = members[name]
}

data class DocumentMember(
    val keyLocation: SourceLocation,
    val value: DocumentNode,
)

class DocumentArray(
    elements: List<DocumentNode>,
    override val location: SourceLocation,
) : DocumentNode {
    val elements: List<DocumentNode> =
        Collections.unmodifiableList(ArrayList(elements))

    operator fun get(index: Int): DocumentNode = elements[index]
}

data class DocumentString(
    val value: String,
    override val location: SourceLocation,
) : DocumentNode

data class DocumentNumber(
    val value: Number,
    override val location: SourceLocation,
) : DocumentNode

data class DocumentBoolean(
    val value: Boolean,
    override val location: SourceLocation,
) : DocumentNode

data class DocumentNull(
    override val location: SourceLocation,
) : DocumentNode

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
