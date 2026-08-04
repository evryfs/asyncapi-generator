package dev.banking.asyncapi.generator.core.document

import com.fasterxml.jackson.core.io.JsonStringEncoder

internal fun appendDocumentMember(parentPath: String, memberName: String): String =
    if (memberName.isSimpleDocumentMember()) {
        if (parentPath.isEmpty()) memberName else "$parentPath.$memberName"
    } else {
        buildString {
            append(parentPath)
            append("[\"")
            append(JsonStringEncoder.getInstance().quoteAsString(memberName))
            append("\"]")
        }
    }

internal fun appendDocumentIndex(parentPath: String, index: Int): String {
    require(index >= 0) { "Document array indexes must be nonnegative" }
    return "$parentPath[$index]"
}

private fun String.isSimpleDocumentMember(): Boolean =
    isNotEmpty() && none { character ->
        character == '.' ||
            character == '[' ||
            character == ']' ||
            character == '"' ||
            character == '\\' ||
            character.isISOControl()
    }
