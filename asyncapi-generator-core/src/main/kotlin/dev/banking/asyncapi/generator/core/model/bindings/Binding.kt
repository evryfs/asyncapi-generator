package dev.banking.asyncapi.generator.core.model.bindings

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

data class Binding(
    @get:JsonAnyGetter val content: Map<String, Any?>,
    @get:JsonIgnore val kafkaKeySchema: SchemaInterface? = null,
    @get:JsonIgnore val protocolBindings: List<ProtocolBinding> = emptyList(),
)

/** Location whose protocol-specific binding vocabulary owns a binding object. */
enum class BindingLocation {
    SERVER,
    CHANNEL,
    OPERATION,
    MESSAGE,
    SCHEMA,
    UNKNOWN,
}

/** Exact protocol binding content plus parser-owned validation metadata. */
data class ProtocolBinding(
    val protocol: String,
    val location: BindingLocation,
    val content: Any?,
    val bindingVersion: Any?,
    val schemaFields: Map<String, SchemaInterface> = emptyMap(),
)
