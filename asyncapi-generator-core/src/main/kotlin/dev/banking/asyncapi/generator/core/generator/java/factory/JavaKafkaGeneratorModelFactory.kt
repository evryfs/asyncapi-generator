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

class JavaKafkaGeneratorModelFactory(
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
                        description = DocumentationUtils.toJavaDocLines("Handler for messages on topic '${channel.topic}'"),
                        methods =
                            listOf(
                                GeneratorItem.HandlerMethod(
                                    methodName = methodName,
                                    payloadType = payload.payloadType,
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
                        description = DocumentationUtils.toJavaDocLines("Spring Kafka Listener for topic '${channel.topic}'"),
                        topic = channel.topic,
                        groupId = "\${spring.kafka.consumer.group-id}",
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
                    )
                val producerName = "${topicPrefix}Producer${payload.messageName}"
                items.add(
                    GeneratorItem.KafkaProducerClass(
                        name = producerName,
                        packageName = producerPackage,
                        description = DocumentationUtils.toJavaDocLines("Producer for topic '${channel.topic}'"),
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
