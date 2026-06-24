package dev.banking.asyncapi.generator.core.generator.kafka.spring

/**
 * Payload signature model used by Spring Kafka client generators.
 */
data class KafkaPayload(
    val messageName: String,
    val payloadType: String,
    val payloadDescription: String? = null,
    val importName: String? = null,
    val headerTypeName: String? = null,
    val headerImportName: String? = null,
    val headerProperties: List<KafkaHeaderProperty> = emptyList(),
)

data class KafkaHeaderProperty(
    val name: String,
    val accessorName: String,
    val description: String? = null,
    val required: Boolean = false,
)
