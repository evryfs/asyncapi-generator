package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils.toKDocLines
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType

class KotlinSpringKafkaSimpleModelFactory(
    private val clientPackage: String,
    private val modelPackage: String,
) {
    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        val items = mutableListOf<GeneratorItem>()
        val baseName = MapperUtil.toPascalCase(channel.channelName)
        val producerPackage = "$clientPackage.producer"
        val consumerPackage = "$clientPackage.consumer"

        val baseImports =
            if (clientPackage != modelPackage) {
                channel.messages.mapNotNull { msg ->
                    val type = resolvePayloadType(msg)
                    if (isPrimitive(type)) null else "$modelPackage.$type"
                }
            } else {
                emptyList()
            }

        if (channel.isConsumer) {
            val consumerName = "${baseName}Consumer"
            val imports = (baseImports + "org.apache.kafka.clients.consumer.ConsumerRecord").distinct().sorted()
            val methods =
                channel.messages.map { msg ->
                    GeneratorItem.HandlerMethod(
                        methodName = "on${msg.name}",
                        payloadType = resolvePayloadType(msg),
                        keyType = "String?",
                    )
                }
            items.add(
                GeneratorItem.KafkaHandlerInterface(
                    name = consumerName,
                    packageName = consumerPackage,
                    description = toKDocLines("Consumer for topic '${channel.topic}'"),
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
            channel.messages.forEach { msg ->
                val payloadType = resolvePayloadType(msg)
                val sendMethod =
                    GeneratorItem.SendMethod(
                        methodName = "send${msg.name}",
                        payloadType = payloadType,
                        keyType = "String",
                    )
                val producerName = "${baseName}Producer${msg.name}"
                items.add(
                    GeneratorItem.KafkaProducerClass(
                        name = producerName,
                        packageName = producerPackage,
                        description = toKDocLines("Producer for topic '${channel.topic}'"),
                        topic = channel.topic,
                        sendMethods = listOf(sendMethod),
                        kafkaValueType = payloadType,
                        imports = imports,
                        topicPropertyKey = "",
                    ),
                )
            }
        }

        return items
    }

    private fun resolvePayloadType(msg: AnalyzedMessage): String =
        when (msg.schema.type.getPrimaryType()) {
            "string" -> "String"
            "integer" -> "Int"
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> msg.name
        }

    private fun isPrimitive(type: String): Boolean = type in setOf("String", "Int", "Long", "Boolean", "java.math.BigDecimal")
}
