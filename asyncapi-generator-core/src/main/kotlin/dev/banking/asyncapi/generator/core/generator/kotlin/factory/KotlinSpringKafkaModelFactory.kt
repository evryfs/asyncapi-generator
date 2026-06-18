package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHeaderProperty
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayload
import dev.banking.asyncapi.generator.core.generator.kafka.spring.NativeKafkaPayloadResolver
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils.toKDocLines
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType

class KotlinSpringKafkaModelFactory(
    private val clientPackage: String,
    private val modelPackage: String,
    private val generateHeaders: Boolean = true,
    private val generateProducers: Boolean = true,
    private val generateConsumers: Boolean = true,
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        val items = mutableListOf<GeneratorItem>()
        val baseName = MapperUtil.toPascalCase(channel.channelName)
        val producerPackage = "$clientPackage.producer"
        val consumerPackage = "$clientPackage.consumer"
        val payloads = channel.payloads()

        val baseImports =
            payloads.flatMap { payload -> listOfNotNull(payload.importName, payload.headerImportName) }
                .distinct()
                .sorted()

        if (channel.isConsumer && generateConsumers) {
            val consumerName = "${baseName}Consumer"
            val imports = (baseImports + "org.apache.kafka.clients.consumer.ConsumerRecord").distinct().sorted()
            val methods =
                payloads.map { payload ->
                    GeneratorItem.HandlerMethod(
                        methodName = "on${payload.messageName}",
                        payloadType = payload.payloadType,
                        keyType = "String?",
                        headerType = payload.headerTypeName,
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

        if (channel.isProducer && generateProducers) {
            val imports =
                (
                    baseImports +
                        "java.util.concurrent.CompletableFuture" +
                        "org.apache.kafka.clients.producer.ProducerRecord" +
                        "org.springframework.kafka.core.KafkaTemplate" +
                        "org.springframework.kafka.support.SendResult"
                )
                    .distinct()
                    .sorted()
            payloads.forEach { payload ->
                val sendMethod =
                    GeneratorItem.SendMethod(
                        methodName = "send${payload.messageName}",
                        payloadType = payload.payloadType,
                        keyType = "String",
                        headerType = payload.headerTypeName,
                        headerProperties =
                            payload.headerProperties.map { header ->
                                GeneratorItem.HeaderProperty(
                                    name = header.name,
                                    accessorName = header.accessorName,
                                )
                            },
                    )
                val producerName = "${baseName}Producer${payload.messageName}"
                items.add(
                    GeneratorItem.KafkaProducerClass(
                        name = producerName,
                        packageName = producerPackage,
                        description = toKDocLines("Producer for topic '${channel.topic}'"),
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
        messages.map(::payload) +
            multiFormatMessages.mapNotNull { message ->
                nativeKafkaPayloadResolver.resolve(message)
                    ?.withHeaders(message.headers)
            }

    private fun payload(msg: AnalyzedMessage): KafkaPayload {
        val type = resolvePayloadType(msg)
        val headers = if (generateHeaders) msg.headers else null
        return KafkaPayload(
            messageName = msg.messageName,
            payloadType = type,
            importName =
                if (isPrimitive(type)) {
                    null
                } else {
                    "$modelPackage.$type"
                },
            headerTypeName = headers?.typeName,
            headerImportName = headers?.typeName?.let { "$clientPackage.header.$it" },
            headerProperties =
                headers
                    ?.properties
                    ?.keys
                    ?.map { headerName ->
                        KafkaHeaderProperty(
                            name = headerName,
                            accessorName = headerName,
                        )
                    }
                    .orEmpty(),
        )
    }

    private fun resolvePayloadType(msg: AnalyzedMessage): String =
        when (msg.schema.type.getPrimaryType()) {
            "string" -> "String"
            "integer" -> "Int"
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> msg.payloadTypeName
        }

    private fun isPrimitive(type: String): Boolean = type in setOf("String", "Int", "Long", "Boolean", "java.math.BigDecimal")

    private fun KafkaPayload.withHeaders(headers: AnalyzedMessageHeaders?): KafkaPayload =
        if (generateHeaders) {
            copy(
                headerTypeName = headers?.typeName,
                headerImportName = headers?.typeName?.let { "$clientPackage.header.$it" },
                headerProperties =
                    headers
                        ?.properties
                        ?.keys
                        ?.map { headerName ->
                            KafkaHeaderProperty(
                                name = headerName,
                                accessorName = headerName,
                            )
                        }
                        .orEmpty(),
            )
        } else {
            this
        }
}
