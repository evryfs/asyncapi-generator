package dev.banking.asyncapi.generator.core.model.bindings

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * A binding: protocol-specific information, written as it appears in the contract.
 *
 * @property content the binding as written
 * @property protocolBindings the binding of each protocol, with its schema-valued fields parsed
 */
internal data class Binding(
    @get:JsonAnyGetter val content: Map<String, Any?>,
    @get:JsonIgnore val protocolBindings: List<ProtocolBinding> = emptyList(),
) {
    /** The schema of the Kafka message key, when the binding has a Kafka binding with a `key`. */
    @get:JsonIgnore
    val kafkaKeySchema: SchemaInterface?
        get() = protocolBindings.firstOrNull { it.protocol == KAFKA_PROTOCOL }?.schemaFields?.get(KEY_FIELD)

    private companion object {
        const val KAFKA_PROTOCOL = "kafka"
        const val KEY_FIELD = "key"
    }
}

/** Location whose protocol-specific binding vocabulary owns a binding object. */
internal enum class BindingLocation {
    SERVER,
    CHANNEL,
    OPERATION,
    MESSAGE,
    SCHEMA,
    UNKNOWN,
}

/** Exact protocol binding content plus parser-owned validation metadata. */
internal data class ProtocolBinding(
    val protocol: String,
    val location: BindingLocation,
    val content: Any?,
    val bindingVersion: Any?,
    val schemaFields: Map<String, SchemaInterface> = emptyMap(),
)
