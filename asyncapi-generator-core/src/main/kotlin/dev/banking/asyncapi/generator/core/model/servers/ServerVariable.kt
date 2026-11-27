package dev.banking.asyncapi.generator.core.model.servers

import com.fasterxml.jackson.annotation.JsonAnyGetter

internal data class ServerVariable(
    val enum: List<String>? = null,
    val default: String? = null,
    val description: String? = null,
    val examples: List<String>? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
