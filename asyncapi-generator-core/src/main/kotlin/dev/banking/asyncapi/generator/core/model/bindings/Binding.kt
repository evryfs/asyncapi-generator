package dev.banking.asyncapi.generator.core.model.bindings

import com.fasterxml.jackson.annotation.JsonAnyGetter

data class Binding(
    @get:JsonAnyGetter val content: Map<String, Any?>
)
