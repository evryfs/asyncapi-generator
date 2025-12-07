package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType

class JavaKafkaGeneratorModelFactory(
    private val packageName: String,
    private val modelPackage: String
) {

    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        val items = mutableListOf<GeneratorItem>()
        val baseName = MapperUtil.toPascalCase(channel.channelName)

        val imports = if (packageName != modelPackage) {
            channel.messages.mapNotNull { msg ->
                val type = resolvePayloadType(msg)
                if (isPrimitive(type)) null else "$modelPackage.$type"
            }.distinct().sorted()
        } else {
            emptyList()
        }

        if (channel.isConsumer) {
            val handlerName = "${baseName}Handler"
            val handlerMethods = channel.messages.map { msg ->
                GeneratorItem.HandlerMethod(
                    methodName = "on${msg.name}",
                    payloadType = resolvePayloadType(msg)
                )
            }
            items.add(
                GeneratorItem.KafkaHandlerInterface(
                    name = handlerName,
                    packageName = packageName,
                    description = DocumentationUtils.toJavaDocLines("Handler for messages on topic '${channel.topic}'"),
                    methods = handlerMethods,
                    imports = imports
                )
            )

            val listenerName = "${baseName}Listener"
            val dispatches = channel.messages.map { msg ->
                GeneratorItem.MessageDispatch(
                    payloadType = resolvePayloadType(msg),
                    methodName = "on${msg.name}"
                )
            }
            items.add(
                GeneratorItem.KafkaListenerClass(
                    name = listenerName,
                    packageName = packageName,
                    description = DocumentationUtils.toJavaDocLines("Spring Kafka Listener for topic '${channel.topic}'"),
                    topic = channel.topic,
                    groupId = "\${spring.kafka.consumer.group-id}",
                    handlerInterface = handlerName,
                    messageDispatches = dispatches,
                    imports = imports
                )
            )
        }

        if (channel.isProducer) {
            val producerName = "${baseName}Producer"
            val sendMethods = channel.messages.map { msg ->
                GeneratorItem.SendMethod(
                    methodName = "send${msg.name}",
                    payloadType = resolvePayloadType(msg)
                )
            }
            items.add(
                GeneratorItem.KafkaProducerClass(
                    name = producerName,
                    packageName = packageName,
                    description = DocumentationUtils.toJavaDocLines("Producer for topic '${channel.topic}'"),
                    topic = channel.topic,
                    sendMethods = sendMethods,
                    imports = imports
                )
            )
        }
        return items
    }

    private fun resolvePayloadType(msg: AnalyzedMessage): String {
        return when (msg.schema.type.getPrimaryType()) {
            "string" -> "String"
            "integer" -> "Integer"
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> msg.name
        }
    }

    private fun isPrimitive(type: String): Boolean {
        return type in setOf("String", "Integer", "Long", "Boolean", "Double", "java.math.BigDecimal")
    }
}
