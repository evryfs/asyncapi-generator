package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayload
import dev.banking.asyncapi.generator.core.generator.kafka.spring.NativeKafkaPayloadResolver
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class JavaSpringKafkaSimpleModelFactory(
    private val clientPackage: String,
    private val modelPackage: String,
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        val items = mutableListOf<GeneratorItem>()
        val baseName = MapperUtil.toPascalCase(channel.channelName)
        val producerPackage = "$clientPackage.producer"
        val consumerPackage = "$clientPackage.consumer"
        val payloads = channel.payloads()

        val baseImports =
            payloads.mapNotNull { payload -> payload.importName }

        if (channel.isConsumer) {
            val consumerName = "${baseName}Consumer"
            val imports = (baseImports + "org.apache.kafka.clients.consumer.ConsumerRecord").distinct().sorted()
            val methods =
                payloads.map { payload ->
                    GeneratorItem.HandlerMethod(
                        methodName = "on${payload.messageName}",
                        payloadType = payload.payloadType,
                    )
                }
            items.add(
                GeneratorItem.KafkaHandlerInterface(
                    name = consumerName,
                    packageName = consumerPackage,
                    description = DocumentationUtils.toJavaDocLines("Consumer for topic '${channel.topic}'"),
                    methods = methods,
                    imports = imports,
                ),
            )
        }

        if (channel.isProducer) {
            val imports =
                (baseImports + "org.apache.kafka.clients.producer.ProducerRecord" + "org.springframework.kafka.core.KafkaTemplate")
                    .distinct()
                    .sorted()
            payloads.forEach { payload ->
                val sendMethod =
                    GeneratorItem.SendMethod(
                        methodName = "send${payload.messageName}",
                        payloadType = payload.payloadType,
                    )
                val producerName = "${baseName}Producer${payload.messageName}"
                items.add(
                    GeneratorItem.KafkaProducerClass(
                        name = producerName,
                        packageName = producerPackage,
                        description = DocumentationUtils.toJavaDocLines("Producer for topic '${channel.topic}'"),
                        topic = channel.topic,
                        sendMethods = listOf(sendMethod),
                        kafkaValueType = payload.payloadType,
                        imports = imports,
                        topicPropertyKey = "",
                    ),
                )
            }
        }

        return items
    }

    private fun AnalyzedChannel.payloads(): List<KafkaPayload> =
        messages.map(::payload) + multiFormatMessages.mapNotNull(nativeKafkaPayloadResolver::resolve)

    private fun payload(msg: AnalyzedMessage): KafkaPayload {
        val type = resolvePayloadType(msg)
        return KafkaPayload(
            messageName = msg.messageName,
            payloadType = type,
            importName =
                if (isPrimitive(type)) {
                    null
                } else {
                    "$modelPackage.$type"
                },
        )
    }

    private fun resolvePayloadType(msg: AnalyzedMessage): String =
        if (isOpenPayloadSchema(msg.schema)) {
            "Object"
        } else {
            when (msg.schema.type.getPrimaryType()) {
                "string" -> "String"
                "integer" -> "Integer"
                "number" -> "java.math.BigDecimal"
                "boolean" -> "Boolean"
                else -> msg.payloadTypeName
            }
        }

    private fun isOpenPayloadSchema(schema: Schema): Boolean {
        if (schema.type == null) {
            return schema.properties.isNullOrEmpty() &&
                schema.additionalProperties == null &&
                schema.enum.isNullOrEmpty() &&
                schema.oneOf.isNullOrEmpty() &&
                schema.anyOf.isNullOrEmpty() &&
                schema.allOf.isNullOrEmpty()
        }
        if (schema.type.getPrimaryType() != "object") return false
        if (!schema.properties.isNullOrEmpty()) return false
        return when (val additional = schema.additionalProperties) {
            null -> true
            is SchemaInterface.BooleanSchema -> additional.value
            is SchemaInterface.SchemaInline ->
                additional.schema.type == null &&
                    additional.schema.properties.isNullOrEmpty() &&
                    additional.schema.additionalProperties == null
            else -> false
        }
    }

    private fun isPrimitive(type: String): Boolean =
        type in setOf("String", "Integer", "Long", "Boolean", "Double", "java.math.BigDecimal", "Object")
}
