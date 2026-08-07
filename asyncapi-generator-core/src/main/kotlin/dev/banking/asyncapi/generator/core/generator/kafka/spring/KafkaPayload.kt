package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Payload signature model used by Spring Kafka client generators.
 */
data class KafkaPayload(
    val messageName: String,
    val javaTypeName: String?,
    val kotlinTypeName: String?,
    val payloadDescription: String? = null,
    val javaImportName: String? = null,
    val kotlinImportName: String? = null,
    val keySchema: SchemaInterface? = null,
    val headerProperties: List<KafkaHeaderProperty> = emptyList(),
) {
    val hasPayload: Boolean get() = javaTypeName != null || kotlinTypeName != null
}

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
