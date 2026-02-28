package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils.toKDocLines
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType

class KotlinKafkaGeneratorModelFactory(
    private val packageName: String,
    private val modelPackage: String,
    private val topicPropertyPrefix: String,
    private val topicPropertySuffix: String,
) {
    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        val items = mutableListOf<GeneratorItem>()
        val baseName = MapperUtil.toPascalCase(channel.channelName)

        val baseImports =
            if (packageName != modelPackage) {
                channel.messages.mapNotNull { msg ->
                    val type = resolvePayloadType(msg)
                    if (isPrimitive(type)) null else "$modelPackage.$type"
                }
            } else {
                emptyList()
            }
        val imports =
            (baseImports + "org.apache.kafka.clients.consumer.ConsumerRecord")
                .distinct()
                .sorted()

        if (channel.isConsumer) {
            val topicPropertyKey = topicPropertyKey(channel.channelName)
            val topicPrefix = "Topic$baseName"
            channel.messages.forEach { msg ->
                val payloadType = resolvePayloadType(msg)
                val methodName = "on${msg.name}"
                val handlerName = "${topicPrefix}Handler${msg.name}"
                items.add(
                    GeneratorItem.KafkaHandlerInterface(
                        name = handlerName,
                        packageName = packageName,
                        description = toKDocLines("Handler for messages on topic '${channel.topic}'"),
                        methods = listOf(
                            GeneratorItem.HandlerMethod(
                                methodName = methodName,
                                payloadType = payloadType,
                                keyType = "String?",
                            )
                        ),
                        imports = imports,
                    )
                )
                val listenerName = "${topicPrefix}Listener${msg.name}"
                items.add(
                    GeneratorItem.KafkaListenerClass(
                        name = listenerName,
                        packageName = packageName,
                        description = toKDocLines("Spring Kafka Listener for topic '${channel.topic}'"),
                        topic = channel.topic,
                        groupId = "\\\${spring.kafka.consumer.group-id}",
                        handlerInterface = handlerName,
                        payloadType = payloadType,
                        methodName = methodName,
                        imports = imports,
                        topicPropertyKey = topicPropertyKey,
                    )
                )
            }
        }

        if (channel.isProducer) {
            val topicPropertyKey = topicPropertyKey(channel.channelName)
            val producerName = "${baseName}Producer"
            val sendMethods =
                channel.messages.map { msg ->
                    GeneratorItem.SendMethod(
                        methodName = "send${msg.name}",
                        payloadType = resolvePayloadType(msg),
                        keyType = "String",
                    )
                }
            val payloadTypes = sendMethods.map { it.payloadType }.distinct()
            val kafkaValueType = if (payloadTypes.size == 1) payloadTypes.first() else "Any"
            items.add(
                GeneratorItem.KafkaProducerClass(
                    name = producerName,
                    packageName = packageName,
                    description = toKDocLines("Producer for topic '${channel.topic}'"),
                    topic = channel.topic,
                    sendMethods = sendMethods,
                    kafkaValueType = kafkaValueType,
                    imports = imports,
                    topicPropertyKey = topicPropertyKey,
                ),
            )
        }
        return items
    }

    private fun topicPropertyKey(channelName: String): String {
        val suffix = if (topicPropertySuffix.isBlank()) "" else ".$topicPropertySuffix"
        return "$topicPropertyPrefix.$channelName$suffix"
    }

    private fun resolvePayloadType(msg: AnalyzedMessage): String =
        when (msg.schema.type.getPrimaryType()) {
            "string" -> "String"
            "integer" -> "Int" // Simplified, could check format for Long
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> msg.name // Object types use the Class Name
        }

    private fun isPrimitive(type: String): Boolean =
        type in setOf("String", "Int", "Long", "Boolean", "java.math.BigDecimal")
}
