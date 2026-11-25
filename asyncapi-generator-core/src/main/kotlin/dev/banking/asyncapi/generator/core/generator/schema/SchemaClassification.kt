package dev.banking.asyncapi.generator.core.generator.schema

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Whether this schema accepts any object: no type and no structure at all, or an
 * object type without properties whose additional properties are unconstrained.
 */
internal fun Schema.isOpenPayload(): Boolean =
    if (type == null) {
        properties.isNullOrEmpty() &&
            additionalProperties == null &&
            enum.isNullOrEmpty() &&
            oneOf.isNullOrEmpty() &&
            anyOf.isNullOrEmpty() &&
            allOf.isNullOrEmpty()
    } else {
        type?.primary == "object" && properties.isNullOrEmpty() && hasUnconstrainedAdditionalProperties()
    }

private fun Schema.hasUnconstrainedAdditionalProperties(): Boolean =
    when (val additional = additionalProperties) {
        null -> true
        is SchemaInterface.BooleanSchema -> additional.value
        is SchemaInterface.SchemaInline ->
            additional.schema.type == null &&
                additional.schema.properties.isNullOrEmpty() &&
                additional.schema.additionalProperties == null
        else -> false
    }

/**
 * Whether this schema is a plain scalar, which needs no generated type of its own: a string, number,
 * integer or boolean schema that is not a string enum. Only string enums become enum types, so an
 * integer or number enum is typed as its scalar.
 */
internal fun Schema.isScalarAlias(): Boolean =
    type?.primary in SCALAR_TYPES && (type?.primary != "string" || enum.isNullOrEmpty())

private val SCALAR_TYPES = setOf("string", "number", "integer", "boolean")

/** Whether this schema is a `oneOf` or `anyOf` union, which gets a generated type of its own. */
internal fun Schema.isInlineUnion(): Boolean = !oneOf.isNullOrEmpty() || !anyOf.isNullOrEmpty()
