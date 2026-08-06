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
