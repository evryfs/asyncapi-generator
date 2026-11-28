package dev.banking.asyncapi.generator.core.model.messages

import com.fasterxml.jackson.annotation.JsonAnyGetter

internal data class MessageExample(
    val headers: Map<String, Any?>? = null,
    val payload: Any? = null,
    val name: String? = null,
    val summary: String? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
