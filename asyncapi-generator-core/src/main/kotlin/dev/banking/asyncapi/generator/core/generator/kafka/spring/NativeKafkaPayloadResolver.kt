package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.avro.NativeAvroPayloadTypeResolver
import dev.banking.asyncapi.generator.core.generator.ir.TypeRef
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.messaging.ResolvedMessagePayload
import dev.banking.asyncapi.generator.core.generator.protobuf.NativeProtobufPayloadTypeResolver

/** Resolves native multi-format payloads into Spring Kafka payload signatures. */
internal class NativeKafkaPayloadResolver(
    private val nativeAvroPayloadTypeResolver: NativeAvroPayloadTypeResolver = NativeAvroPayloadTypeResolver(),
    private val nativeProtobufPayloadTypeResolver: NativeProtobufPayloadTypeResolver =
        NativeProtobufPayloadTypeResolver(),
) {
    fun resolve(
        message: AnalyzedMessage,
        payload: ResolvedMessagePayload.MultiFormat,
    ): KafkaPayload? =
        nativeAvroPayloadTypeResolver.resolve(payload)?.let { payloadType ->
            KafkaPayload(
                messageName = message.messageName,
                type = TypeRef.External(name = payloadType.typeName, packageName = payloadType.packageName),
                kafkaKey = message.kafkaKey,
            )
        } ?: nativeProtobufPayloadTypeResolver.resolve(payload)?.let { payloadType ->
            KafkaPayload(
                messageName = message.messageName,
                type = TypeRef.External(name = payloadType.typeName, packageName = payloadType.packageName),
                kafkaKey = message.kafkaKey,
            )
        }
}
