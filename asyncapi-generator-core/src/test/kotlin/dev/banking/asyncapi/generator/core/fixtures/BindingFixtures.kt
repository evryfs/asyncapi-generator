package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/** A message binding whose Kafka binding has the message key schema [key]. */
internal fun kafkaKeyBinding(key: SchemaInterface): Binding =
    Binding(
        content = emptyMap(),
        protocolBindings =
            listOf(
                ProtocolBinding(
                    protocol = "kafka",
                    location = BindingLocation.MESSAGE,
                    content = emptyMap<String, Any?>(),
                    bindingVersion = null,
                    schemaFields = mapOf("key" to key),
                ),
            ),
    )
