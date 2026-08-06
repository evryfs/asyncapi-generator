package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.model.schemas.Schema

internal object JsonPropertyAccessAnnotationMapper {
    fun annotationFor(schema: Schema?): String? =
        when {
            schema?.readOnly == true && schema.writeOnly == true -> "@JsonProperty(access = Access.READ_WRITE)"
            schema?.readOnly == true -> "@JsonProperty(access = Access.READ_ONLY)"
            schema?.writeOnly == true -> "@JsonProperty(access = Access.WRITE_ONLY)"
            else -> null
        }
}
