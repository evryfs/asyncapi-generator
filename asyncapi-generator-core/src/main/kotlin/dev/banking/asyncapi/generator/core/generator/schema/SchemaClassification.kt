package dev.banking.asyncapi.generator.core.generator.schema

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

internal fun Schema.isOpenPayload(): Boolean {
    if (type == null) {
        return properties.isNullOrEmpty() &&
            additionalProperties == null &&
            enum.isNullOrEmpty() &&
            oneOf.isNullOrEmpty() &&
            anyOf.isNullOrEmpty() &&
            allOf.isNullOrEmpty()
    }
    if (type.getPrimaryType() != "object" || !properties.isNullOrEmpty()) {
        return false
    }
    return when (val additional = additionalProperties) {
        null -> true
        is SchemaInterface.BooleanSchema -> additional.value
        is SchemaInterface.SchemaInline ->
            additional.schema.type == null &&
                additional.schema.properties.isNullOrEmpty() &&
                additional.schema.additionalProperties == null
        else -> false
    }
}

internal fun Schema.isScalarAlias(): Boolean {
    if (!enum.isNullOrEmpty()) {
        return false
    }
    return type.getPrimaryType() in setOf("string", "number", "integer", "boolean")
}
