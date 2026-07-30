package dev.banking.asyncapi.generator.core.model.bindings

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

data class Binding(
    @get:JsonAnyGetter val content: Map<String, Any?>,
    @get:JsonIgnore val kafkaKeySchema: SchemaInterface? = null,
)
