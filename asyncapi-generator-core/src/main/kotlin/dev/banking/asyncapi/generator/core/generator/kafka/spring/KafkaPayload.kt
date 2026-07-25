package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Payload signature model used by Spring Kafka client generators.
 */
data class KafkaPayload(
    val messageName: String,
    val payloadType: String,
    val payloadDescription: String? = null,
    val importName: String? = null,
    val keySchema: SchemaInterface? = null,
    val headerTypeName: String? = null,
    val headerImportName: String? = null,
    val headerProperties: List<KafkaHeaderProperty> = emptyList(),
)

data class KafkaHeaderProperty(
    val wireName: String,
    val parameterName: String,
    val javaTypeName: String,
    val kotlinTypeName: String,
    val importName: String? = null,
    val description: String? = null,
    val required: Boolean = false,
    val nullable: Boolean = !required,
)
