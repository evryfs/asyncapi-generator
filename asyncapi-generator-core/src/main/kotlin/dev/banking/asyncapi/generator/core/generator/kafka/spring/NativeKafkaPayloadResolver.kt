package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMultiFormatMessage
import dev.banking.asyncapi.generator.core.generator.avro.NativeAvroPayloadTypeResolver
import dev.banking.asyncapi.generator.core.generator.protobuf.NativeProtobufPayloadTypeResolver

/**
 * Resolves native multi-format payloads into Spring Kafka payload signatures.
 *
 * Expected behavior is covered by:
 * - `GenerateKotlinSpringKafkaTest`
 * - `GenerateJavaSpringKafkaTest`
 */
class NativeKafkaPayloadResolver(
    private val nativeAvroPayloadTypeResolver: NativeAvroPayloadTypeResolver = NativeAvroPayloadTypeResolver(),
    private val nativeProtobufPayloadTypeResolver: NativeProtobufPayloadTypeResolver = NativeProtobufPayloadTypeResolver(),
) {
    fun resolve(message: AnalyzedMultiFormatMessage): KafkaPayload? =
        nativeAvroPayloadTypeResolver.resolve(message)?.let { payloadType ->
            KafkaPayload(
                messageName = message.messageName,
                payloadType = payloadType.typeName,
                importName = payloadType.importName,
            )
        } ?: nativeProtobufPayloadTypeResolver.resolve(message)?.let { payloadType ->
            KafkaPayload(
                messageName = message.messageName,
                payloadType = payloadType.typeName,
                importName = payloadType.importName,
            )
        }
}
