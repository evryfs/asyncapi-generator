package dev.banking.asyncapi.generator.core.reader

import java.util.Collections

/**
 * Format-independent value read from an input document.
 *
 * Every node retains the location of its source value. Object members also
 * retain the location of their key through [DocumentMember].
 *
 * Expected behavior is covered by:
 * - `DocumentNodeTest`
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
