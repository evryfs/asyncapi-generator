package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.ir.TypeRef
import dev.banking.asyncapi.generator.core.generator.messaging.kafka.KafkaKey

/**
 * The payload signature of a message in a Spring Kafka client contract.
 *
 * @param messageName Source-code name of the message.
 * @param type Type of the payload parameter, or `null` when the message has no payload.
 * @param payloadDescription Description of the payload schema.
 * @param kafkaKey Kafka record key of the message, or `null` when it declares none.
 * @param headerProperties Header parameters of the message.
 */
internal data class KafkaPayload(
    val messageName: String,
    val type: TypeRef?,
    val payloadDescription: String? = null,
    val kafkaKey: KafkaKey? = null,
    val headerProperties: List<KafkaHeaderProperty> = emptyList(),
) {
    val hasPayload: Boolean get() = type != null
}

/**
 * A header parameter of a Spring Kafka client method.
 *
 * @param wireName Name of the header on the Kafka record.
 * @param parameterName Source-code name of the parameter.
 * @param type Type of the parameter.
 * @param description Description of the header schema.
 * @param required Whether the contract requires the header.
 * @param nullable Whether the parameter accepts `null`.
 */
internal data class KafkaHeaderProperty(
    val wireName: String,
    val parameterName: String,
    val type: TypeRef.Scalar,
    val description: String? = null,
    val required: Boolean = false,
    val nullable: Boolean = !required,
)
