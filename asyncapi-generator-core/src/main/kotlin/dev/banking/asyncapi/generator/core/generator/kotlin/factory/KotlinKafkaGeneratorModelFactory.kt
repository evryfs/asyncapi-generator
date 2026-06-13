package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayload
import dev.banking.asyncapi.generator.core.generator.kafka.spring.NativeKafkaPayloadResolver
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils.toKDocLines
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType

class KotlinKafkaGeneratorModelFactory(
    private val packageName: String,
    private val modelPackage: String,
    private val topicPropertyPrefix: String,
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        val items = mutableListOf<GeneratorItem>()
        val baseName = MapperUtil.toPascalCase(channel.channelName)
        val handlerPackage = "$packageName.handler"
        val listenerPackage = "$packageName.listener"
        val producerPackage = "$packageName.producer"
        val payloads = channel.payloads()

        val baseImports =
            payloads.mapNotNull { payload -> payload.importName }
        val imports =
            (baseImports + "org.apache.kafka.clients.consumer.ConsumerRecord")
                .distinct()
                .sorted()

        if (channel.isConsumer) {
            val topicPropertyKey = topicPropertyKey(channel.channelName)
            val topicPrefix = "Topic$baseName"
            payloads.forEach { payload ->
                val methodName = "on${payload.messageName}"
                val handlerName = "${topicPrefix}Handler${payload.messageName}"
                items.add(
                    GeneratorItem.KafkaHandlerInterface(
                        name = handlerName,
                        packageName = handlerPackage,
                        description = toKDocLines("Handler for messages on topic '${channel.topic}'"),
                        methods =
                            listOf(
                                GeneratorItem.HandlerMethod(
                                    methodName = methodName,
                                    payloadType = payload.payloadType,
                                    keyType = "String?",
                                ),
                            ),
                        imports = imports,
                    ),
                )
                val listenerName = "${topicPrefix}Listener${payload.messageName}"
                val listenerImports = (imports + "$handlerPackage.$handlerName").distinct().sorted()
                items.add(
                    GeneratorItem.KafkaListenerClass(
                        name = listenerName,
                        packageName = listenerPackage,
                        description = toKDocLines("Spring Kafka Listener for topic '${channel.topic}'"),
                        topic = channel.topic,
                        groupId = "\\\${spring.kafka.consumer.group-id}",
                        handlerInterface = handlerName,
                        payloadType = payload.payloadType,
                        methodName = methodName,
                        imports = listenerImports,
                        topicPropertyKey = topicPropertyKey,
                    ),
                )
            }
        }

        if (channel.isProducer) {
            val topicPropertyKey = topicPropertyKey(channel.channelName)
            val topicPrefix = "Topic$baseName"
            payloads.forEach { payload ->
                val sendMethod =
                    GeneratorItem.SendMethod(
                        methodName = "send${payload.messageName}",
                        payloadType = payload.payloadType,
                        keyType = "String",
                    )
                val producerName = "${topicPrefix}Producer${payload.messageName}"
                items.add(
                    GeneratorItem.KafkaProducerClass(
                        name = producerName,
                        packageName = producerPackage,
                        description = toKDocLines("Producer for topic '${channel.topic}'"),
                        topic = channel.topic,
                        sendMethods = listOf(sendMethod),
                        kafkaValueType = payload.payloadType,
                        imports = imports,
                        topicPropertyKey = topicPropertyKey,
                    ),
                )
            }
        }
        return items
    }

    private fun topicPropertyKey(channelName: String): String = "$topicPropertyPrefix.$channelName"

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
        when (msg.schema.type.getPrimaryType()) {
            "string" -> "String"
            "integer" -> "Int" // Simplified, could check format for Long
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> msg.payloadTypeName // Object types use the Class Name
        }

    private fun isPrimitive(type: String): Boolean = type in setOf("String", "Int", "Long", "Boolean", "java.math.BigDecimal")
}
