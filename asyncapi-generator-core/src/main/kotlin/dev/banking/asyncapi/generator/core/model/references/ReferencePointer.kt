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

private fun String.decodeJsonPointerSegment(): String =
    replace("~1", "/")
        .replace("~0", "~")
