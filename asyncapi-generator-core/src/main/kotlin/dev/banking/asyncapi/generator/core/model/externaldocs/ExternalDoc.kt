package dev.banking.asyncapi.generator.core.model.externaldocs

import com.fasterxml.jackson.annotation.JsonAnyGetter

data class ExternalDoc(
    val url: String,
    val description: String? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
