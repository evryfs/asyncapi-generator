package dev.banking.asyncapi.generator.core.model.parameters

import com.fasterxml.jackson.annotation.JsonAnyGetter

internal data class Parameter(
    val description: String? = null,
    val location: String? = null,
    val enum: List<String>? = null,
    val default: String? = null,
    val examples: List<String>? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
