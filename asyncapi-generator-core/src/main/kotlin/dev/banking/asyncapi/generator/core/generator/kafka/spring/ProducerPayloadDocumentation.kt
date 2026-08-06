package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType

internal fun AdditionalProducerPayloadType.serializedPayloadDescription(): List<String> =
    when (this) {
        AdditionalProducerPayloadType.BYTE_ARRAY ->
            listOf(
                "Final serialized Kafka record value as a byte array.",
                "The bytes must conform to the payload schema and content type declared by the AsyncAPI contract.",
                "The generated contract does not serialize or validate the byte content.",
                "A compatible serializer such as ByteArraySerializer is application-owned.",
            )
        AdditionalProducerPayloadType.STRING ->
            listOf(
                "Final serialized textual Kafka record value.",
                "The text must conform to the payload schema and content type declared by the AsyncAPI contract.",
                "The generated contract does not serialize or validate the text.",
                "Encoding and serializer configuration are application-owned; StringSerializer uses UTF-8 by default.",
            )
    }
