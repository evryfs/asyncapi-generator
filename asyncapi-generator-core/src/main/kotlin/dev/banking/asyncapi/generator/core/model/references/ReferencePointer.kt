package dev.banking.asyncapi.generator.core.model.references

private const val COMPONENT_SCHEMA_POINTER_PREFIX = "/components/schemas/"

internal fun String.componentSchemaNameOrNull(): String? {
    val pointer = substringAfter('#', missingDelimiterValue = "")
    if (!pointer.startsWith(COMPONENT_SCHEMA_POINTER_PREFIX)) {
        return null
    }

    return pointer
        .removePrefix(COMPONENT_SCHEMA_POINTER_PREFIX)
        .substringBefore('/')
        .takeIf(String::isNotBlank)
        ?.decodeJsonPointerSegment()
}

internal fun String.referenceTargetNameOrNull(): String? =
    substringAfter('#', missingDelimiterValue = "")
        .removePrefix("/")
        .substringAfterLast('/')
        .takeIf(String::isNotBlank)
        ?.decodeJsonPointerSegment()

internal fun String.isExternalReference(): Boolean =
    substringBefore('#').isNotBlank()

internal fun componentSchemaReference(name: String): String =
    "#/components/schemas/${name.encodeJsonPointerSegment()}"

private fun String.decodeJsonPointerSegment(): String =
    replace("~1", "/")
        .replace("~0", "~")

private fun String.encodeJsonPointerSegment(): String =
    replace("~", "~0")
        .replace("/", "~1")
