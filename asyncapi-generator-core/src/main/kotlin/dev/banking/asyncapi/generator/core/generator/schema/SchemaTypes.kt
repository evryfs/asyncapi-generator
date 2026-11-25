package dev.banking.asyncapi.generator.core.generator.schema

/**
 * The first type of a schema `type` other than `null`: the type itself when it is a single
 * string, or the first non-null entry of a type list. Quotes and YAML block markers that the
 * parser kept are ignored.
 */
internal fun Any?.getPrimaryType(): String? =
    when (this) {
        is String -> normalizeSchemaType()
        is List<*> ->
            this
                .filterIsInstance<String>()
                .map { it.normalizeSchemaType() }
                .firstOrNull { !it.equals("null", ignoreCase = true) }

        else -> null
    }

/** Whether a schema `type` allows `null`. */
internal fun Any?.isTypeNullable(): Boolean =
    when (this) {
        is String -> normalizeSchemaType().equals("null", ignoreCase = true)
        is List<*> ->
            this
                .filterIsInstance<String>()
                .any { it.normalizeSchemaType().equals("null", ignoreCase = true) }

        else -> false
    }

/** Whether a schema `type` list names more than one type other than `null`. */
internal fun Any?.hasMultipleNonNullTypes(): Boolean {
    val types =
        (this as? List<*>)
            ?.filterIsInstance<String>()
            ?.map { it.normalizeSchemaType() }
            ?.filter { !it.equals("null", ignoreCase = true) }
            ?: return false
    return types.distinct().size > 1
}

private fun String.normalizeSchemaType(): String =
    trim()
        .trimStart('"', '\'', '|', '>')
        .trimEnd('"', '\'')
