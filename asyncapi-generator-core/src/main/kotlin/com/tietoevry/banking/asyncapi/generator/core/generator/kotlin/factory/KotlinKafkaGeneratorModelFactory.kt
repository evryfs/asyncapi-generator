package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.factory

import com.tietoevry.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import com.tietoevry.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import com.tietoevry.banking.asyncapi.generator.core.generator.util.DocumentationUtils.toKDocLines
import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil
import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType

class KotlinKafkaGeneratorModelFactory(
    private val packageName: String,
    private val modelPackage: String,
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
                    payloadType = resolvePayloadType(msg),
                    keyType = "String?"
                )
            }
            items.add(
                GeneratorItem.KafkaHandlerInterface(
                    name = handlerName,
                    packageName = packageName,
                    description = toKDocLines("Handler for messages on topic '${channel.topic}'"),
                    methods = handlerMethods,
                    imports = imports,
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
                    description = toKDocLines("Spring Kafka Listener for topic '${channel.topic}'"),
                    topic = channel.topic,
                    groupId = "\\\${spring.kafka.consumer.group-id}",
                    handlerInterface = handlerName,
                    messageDispatches = dispatches,
                    imports = imports,
                )
            )
        }

        if (channel.isProducer) {
            val producerName = "${baseName}Producer"
            val sendMethods = channel.messages.map { msg ->
                GeneratorItem.SendMethod(
                    methodName = "send${msg.name}",
                    payloadType = resolvePayloadType(msg),
                    keyType = "String"
                )
            }
            items.add(
                GeneratorItem.KafkaProducerClass(
                    name = producerName,
                    packageName = packageName,
                    description = toKDocLines("Producer for topic '${channel.topic}'"),
                    topic = channel.topic,
                    sendMethods = sendMethods,
                    imports = imports,
                )
            )
        }
        return items
    }

    private fun resolvePayloadType(msg: AnalyzedMessage): String {
        return when (msg.schema.type.getPrimaryType()) {
            "string" -> "String"
            "integer" -> "Int" // Simplified, could check format for Long
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> msg.name // Object types use the Class Name
        }
    }

    private fun isPrimitive(type: String): Boolean {
        return type in setOf("String", "Int", "Long", "Boolean", "java.math.BigDecimal")
    }
}
