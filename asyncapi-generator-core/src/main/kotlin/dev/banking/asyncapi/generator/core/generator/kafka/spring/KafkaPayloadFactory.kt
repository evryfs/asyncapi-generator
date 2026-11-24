package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.ir.ModelTypeMapper
import dev.banking.asyncapi.generator.core.generator.ir.TypeRef
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.schema.isOpenPayload
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType

/**
 * Prepares message payload contracts shared by Java and Kotlin Spring Kafka generation.
 *
 * Language-specific rendering remains responsible for syntax, while payload identity,
 * model imports, headers, and native-schema resolution are derived once here.
 *
 * @param nativeKafkaPayloadResolver Resolves payloads in native schema formats.
 */
internal class KafkaPayloadFactory(
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    fun create(channel: AnalyzedChannel): List<KafkaPayload> =
        channel.messages.mapNotNull { message ->
            when (val payload = message.nativePayload) {
                null -> create(message)
                else ->
                    nativeKafkaPayloadResolver.resolve(message, payload)?.copy(
                        headerProperties = KafkaHeaderPropertyFactory.create(message.headers, message.messageName),
                    )
            }
        }

    private val typeMapper = ModelTypeMapper(emptyMap())

    private fun create(message: AnalyzedMessage): KafkaPayload =
        KafkaPayload(
            messageName = message.messageName,
            type = payloadType(message),
            payloadDescription = message.schema?.description,
            kafkaKey = message.kafkaKey,
            headerProperties = KafkaHeaderPropertyFactory.create(message.headers, message.messageName),
        )

    /**
     * The type of a message payload. Array and scalar payloads generate no model of their own and use the
     * list and scalar types models use; any other payload is its generated model.
     */
    private fun payloadType(message: AnalyzedMessage): TypeRef? =
        message.schema?.let { schema ->
            if (schema.type.getPrimaryType() == "array") {
                typeMapper.map(message.messageName, schema)
            } else {
                ModelTypeMapper.scalarType(schema)
                    ?: TypeRef.Named(
                        name = requireNotNull(message.payload).typeName,
                        isModel = true,
                        isOpenObject = schema.isOpenPayload(),
                    )
            }
        }
}
