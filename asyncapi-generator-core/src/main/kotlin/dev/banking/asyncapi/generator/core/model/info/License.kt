package dev.banking.asyncapi.generator.core.model.info

import com.fasterxml.jackson.annotation.JsonAnyGetter

data class License(
    val name: String,
    val url: String? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
