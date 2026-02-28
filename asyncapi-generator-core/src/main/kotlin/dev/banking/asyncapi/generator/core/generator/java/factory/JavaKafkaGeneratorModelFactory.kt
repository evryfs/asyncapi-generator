package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType

class JavaKafkaGeneratorModelFactory(
    private val packageName: String,
    private val modelPackage: String,
    private val topicPropertyPrefix: String,
    private val topicPropertySuffix: String,
) {
    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        val items = mutableListOf<GeneratorItem>()
        val baseName = MapperUtil.toPascalCase(channel.channelName)
        val handlerPackage = "$packageName.handler"
        val listenerPackage = "$packageName.listener"
        val producerPackage = "$packageName.producer"

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
                        packageName = handlerPackage,
                        description = DocumentationUtils.toJavaDocLines("Handler for messages on topic '${channel.topic}'"),
                        methods =
                            listOf(
                                GeneratorItem.HandlerMethod(
                                    methodName = methodName,
                                    payloadType = payloadType,
                                ),
                            ),
                        imports = imports,
                    ),
                )
                val listenerName = "${topicPrefix}Listener${msg.name}"
                val listenerImports = (imports + "$handlerPackage.$handlerName").distinct().sorted()
                items.add(
                    GeneratorItem.KafkaListenerClass(
                        name = listenerName,
                        packageName = listenerPackage,
                        description = DocumentationUtils.toJavaDocLines("Spring Kafka Listener for topic '${channel.topic}'"),
                        topic = channel.topic,
                        groupId = "\${spring.kafka.consumer.group-id}",
                        handlerInterface = handlerName,
                        payloadType = payloadType,
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
            channel.messages.forEach { msg ->
                val payloadType = resolvePayloadType(msg)
                val sendMethod =
                    GeneratorItem.SendMethod(
                        methodName = "send${msg.name}",
                        payloadType = payloadType,
                    )
                val producerName = "${topicPrefix}Producer${msg.name}"
                items.add(
                    GeneratorItem.KafkaProducerClass(
                        name = producerName,
                        packageName = producerPackage,
                        description = DocumentationUtils.toJavaDocLines("Producer for topic '${channel.topic}'"),
                        topic = channel.topic,
                        sendMethods = listOf(sendMethod),
                        kafkaValueType = payloadType,
                        imports = imports,
                        topicPropertyKey = topicPropertyKey,
                    ),
                )
            }
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
            "integer" -> "Integer"
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> msg.name
        }

    private fun isPrimitive(type: String): Boolean = type in setOf("String", "Integer", "Long", "Boolean", "Double", "java.math.BigDecimal")
}
