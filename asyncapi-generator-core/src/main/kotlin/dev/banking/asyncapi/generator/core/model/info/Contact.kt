package dev.banking.asyncapi.generator.core.model.info

import com.fasterxml.jackson.annotation.JsonAnyGetter

internal data class Contact(
    val name: String? = null,
    val url: String? = null,
    val email: String? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
