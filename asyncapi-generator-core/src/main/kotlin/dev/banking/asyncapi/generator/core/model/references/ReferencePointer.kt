package dev.banking.asyncapi.generator.core.model.references

import com.fasterxml.jackson.core.JsonPointer
import java.io.File
import java.net.URI

internal data class ParsedReference(
    val documentUri: URI?,
    val pointer: JsonPointer,
) {
    val isExternal: Boolean
        get() = documentUri != null

    fun resolveDocumentAgainst(sourceFile: File): File? {
        val uri = documentUri ?: return null
        require(uri.scheme == null || uri.scheme.equals("file", ignoreCase = true)) {
            "Unsupported reference URI scheme '${uri.scheme}'"
        }
        require(uri.rawQuery == null) {
            "Reference document URIs must not contain a query"
        }

        val resolved = sourceFile.parentFile.toURI().resolve(uri)
        require(resolved.scheme.equals("file", ignoreCase = true)) {
            "Unsupported reference URI scheme '${resolved.scheme}'"
        }
        return File(resolved).canonicalFile
    }

    fun pointerSegments(): List<String> = buildList {
        var current = pointer
        while (!current.matches()) {
            add(current.matchingProperty)
            current = current.tail()
        }
    }
}

internal fun String.parseReference(): ParsedReference {
    val uri = URI(trim())
    val fragment = uri.fragment.orEmpty()
    require(fragment.isEmpty() || fragment.startsWith('/')) {
        "Reference fragments must be empty or contain a JSON Pointer starting with '/'"
    }

    val pointer = JsonPointer.compile(fragment)
    val hasDocument = uri.scheme != null || uri.rawAuthority != null || uri.rawPath.isNotEmpty()
    val documentUri =
        if (hasDocument) {
            URI(uri.scheme, uri.authority, uri.path, uri.query, null)
        } else {
            null
        }
    return ParsedReference(documentUri, pointer)
}

internal fun String.componentSchemaNameOrNull(): String? {
    val segments = runCatching { parseReference().pointerSegments() }.getOrNull() ?: return null
    return segments
        .takeIf { it.size >= 3 && it[0] == "components" && it[1] == "schemas" }
        ?.get(2)
        ?.takeIf(String::isNotBlank)
}

internal fun String.referenceTargetNameOrNull(): String? =
    runCatching { parseReference().pointerSegments().lastOrNull() }
        .getOrNull()
        ?.takeIf(String::isNotBlank)

internal fun String.isExternalReference(): Boolean =
    runCatching { parseReference().isExternal }.getOrDefault(false)

internal fun componentSchemaReference(name: String): String =
    "#/components/schemas/${name.encodeJsonPointerSegment()}"

private fun String.encodeJsonPointerSegment(): String =
    replace("~", "~0")
        .replace("/", "~1")
