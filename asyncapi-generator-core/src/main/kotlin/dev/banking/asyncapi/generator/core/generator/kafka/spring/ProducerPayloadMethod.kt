package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType

internal val AdditionalProducerPayloadType.methodSuffix: String
    get() =
        when (this) {
            AdditionalProducerPayloadType.BYTE_ARRAY -> "ByteArray"
            AdditionalProducerPayloadType.STRING -> "String"
        }

internal fun Set<AdditionalProducerPayloadType>.inCanonicalOrder(): List<AdditionalProducerPayloadType> =
    AdditionalProducerPayloadType.entries.filter { type -> type in this }

internal fun producerPayloadMethods(
    messageName: String,
    hasPayload: Boolean,
    additionalPayloadTypes: Set<AdditionalProducerPayloadType>,
): List<ProducerPayloadMethod> {
    val configuredAdditionalTypes =
        if (hasPayload) {
            additionalPayloadTypes.inCanonicalOrder()
        } else {
            emptyList()
        }

    return (listOf(null) + configuredAdditionalTypes).map { additionalPayloadType ->
        ProducerPayloadMethod(
            methodName = "send$messageName${additionalPayloadType?.methodSuffix.orEmpty()}",
            additionalPayloadType = additionalPayloadType,
        )
    }
}

/**
 * A send method of a message.
 *
 * @param methodName Name of the method.
 * @param additionalPayloadType Serialized payload type the method sends, or `null` for the contract payload.
 */
internal data class ProducerPayloadMethod(
    val methodName: String,
    val additionalPayloadType: AdditionalProducerPayloadType?,
)
