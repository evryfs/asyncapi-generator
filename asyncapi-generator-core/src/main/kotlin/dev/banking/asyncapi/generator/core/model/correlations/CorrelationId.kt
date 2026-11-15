package dev.banking.asyncapi.generator.core.model.correlations

import com.fasterxml.jackson.annotation.JsonAnyGetter

data class CorrelationId(
    val location: String,
    val description: String? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
