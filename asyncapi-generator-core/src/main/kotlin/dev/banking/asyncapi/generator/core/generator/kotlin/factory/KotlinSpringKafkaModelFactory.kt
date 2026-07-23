package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHeaderProperty
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayload
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaTopicAddress
import dev.banking.asyncapi.generator.core.generator.kafka.spring.NativeKafkaPayloadResolver
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils.toKDocLines
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class KotlinSpringKafkaModelFactory(
    private val clientPackage: String,
    private val modelPackage: String,
    private val generateHeaders: Boolean = true,
    private val generateProducers: Boolean = true,
    private val generateConsumers: Boolean = true,
    private val topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    private val validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        if (!channel.shouldGenerateClient()) {
            return emptyList()
        }

        val items = mutableListOf<GeneratorItem>()
        val baseName = MapperUtil.toPascalCase(channel.channelName)
        val producerPackage = "$clientPackage.producer"
        val consumerPackage = "$clientPackage.consumer"
        val payloads = channel.payloads()
        val topicAddress =
            KafkaTopicAddress.from(
                channelName = channel.channelName,
                value = channel.topic,
                topicParameterProperties = topicParameterProperties,
            )

        if (channel.isConsumer && generateConsumers) {
            val consumerName = "${baseName}Consumer"
            val imports =
                (
                    payloads.mapNotNull { payload -> payload.importName } +
                        "org.springframework.kafka.support.KafkaHeaders" +
                        "org.springframework.messaging.handler.annotation.Header" +
                        "org.springframework.messaging.handler.annotation.Payload" +
                        listOfNotNull(
                            validationAnnotations.clientContract?.value,
                            validationAnnotations.payloadParameter?.value,
                        )
                )
                    .distinct()
                    .sorted()
            val methods =
                payloads.map { payload ->
                    GeneratorItem.ConsumerMethod(
                        messageName = payload.messageName,
                        methodName =
                            payload.methodName(
                                singleName = "listen",
                                multiplePrefix = "listen",
                                messageCount = payloads.size,
                            ),
                        payloadType = payload.payloadType,
                        payloadDescription =
                            toKDocLines(payload.payloadDescription)
                                .ifEmpty { listOf("Message payload.") },
                        keyDescription =
                            listOf(
                                "Kafka record key, or `null` when the record has no key.",
                            ),
                        keyType = "String?",
                        headerType = payload.headerTypeName,
                        headerProperties =
                            payload.headerProperties.map { header ->
                                GeneratorItem.HeaderProperty(
                                    name = header.name,
                                    accessorName = header.accessorName,
                                    parameterName = header.accessorName,
                                    typeName = header.kotlinTypeName(),
                                    description =
                                        toKDocLines(header.description)
                                            .ifEmpty { listOf("Kafka message header.") },
                                    required = header.required,
                                    defaultValue = if (header.required) null else "null",
                                )
                            },
                        payloadParameterAnnotation = validationAnnotations.payloadParameter?.simpleName,
                    )
                }
            items.add(
                GeneratorItem.KafkaConsumerInterface(
                    name = consumerName,
                    packageName = consumerPackage,
                    description =
                        toKDocLines(
                            "Defines the Spring Kafka consumer contract for messages received from the " +
                                "`${channel.topic}` AsyncAPI channel.",
                        ),
                    topicAddressConstantName = topicAddress.constantName,
                    topicAddress = topicAddress.propertyPlaceholderValue.toKotlinStringLiteral(),
                    methods = methods,
                    clientContractAnnotation = validationAnnotations.clientContract?.simpleName,
                    imports = imports,
                ),
            )
        }

        if (channel.isProducer && generateProducers) {
            val imports =
                (
                    payloads.mapNotNull { payload -> payload.importName } +
                        "java.util.concurrent.CompletableFuture" +
                        "org.apache.kafka.clients.producer.RecordMetadata" +
                        "org.springframework.kafka.support.KafkaHeaders" +
                        "org.springframework.messaging.handler.annotation.Header" +
                        "org.springframework.messaging.handler.annotation.Payload" +
                        listOfNotNull(
                            validationAnnotations.clientContract?.value,
                            validationAnnotations.payloadParameter?.value,
                        )
                )
                    .distinct()
                    .sorted()
            val sendMethods =
                payloads.map { payload ->
                    GeneratorItem.SendMethod(
                        methodName =
                            payload.methodName(
                                singleName = "send",
                                multiplePrefix = "send",
                                messageCount = payloads.size,
                            ),
                        payloadType = payload.payloadType,
                        payloadDescription =
                            toKDocLines(payload.payloadDescription)
                                .ifEmpty { listOf("Message payload.") },
                        keyDescription = listOf("Kafka record key."),
                        keyType = "String",
                        headerType = payload.headerTypeName,
                        headerProperties =
                            payload.headerProperties.map { header ->
                                GeneratorItem.HeaderProperty(
                                    name = header.name,
                                    accessorName = header.accessorName,
                                    parameterName = header.accessorName,
                                    typeName = header.kotlinTypeName(),
                                    description =
                                        toKDocLines(header.description)
                                            .ifEmpty { listOf("Kafka message header.") },
                                    required = header.required,
                                    defaultValue = if (header.required) null else "null",
                                )
                            },
                        payloadParameterAnnotation = validationAnnotations.payloadParameter?.simpleName,
                    )
                }
            items.add(
                GeneratorItem.KafkaProducerClass(
                    name = "${baseName}Producer",
                    packageName = producerPackage,
                    description =
                        toKDocLines(
                            "Producer contract for publishing messages to the `${channel.topic}` topic.",
                        ) +
                            toKDocLines(
                                "The contract exposes the Kafka record key, message payload, and " +
                                    "contract-defined headers as method parameters.",
                            ),
                    topicAddressConstantName = topicAddress.constantName,
                    topicAddress = topicAddress.propertyPlaceholderValue.toKotlinStringLiteral(),
                    sendMethods = sendMethods,
                    clientContractAnnotation = validationAnnotations.clientContract?.simpleName,
                    imports = imports,
                ),
            )
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
            payloadDescription = msg.schema.description,
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
                        val schema = headers.properties.getValue(headerName)
                        KafkaHeaderProperty(
                            name = headerName,
                            accessorName = headerName.toParameterName(),
                            description = schema.description(),
                            required = headerName in headers.requiredProperties,
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
                            val schema = headers.properties.getValue(headerName)
                            KafkaHeaderProperty(
                                name = headerName,
                                accessorName = headerName.toParameterName(),
                                description = schema.description(),
                                required = headerName in headers.requiredProperties,
                            )
                        }
                        .orEmpty(),
            )
        } else {
            this
        }

    private fun KafkaHeaderProperty.kotlinTypeName(): String {
        val nullableSuffix = if (required) "" else "?"
        return "String$nullableSuffix"
    }

    private fun SchemaInterface.description(): String? = resolvedSchema()?.description

    private fun SchemaInterface.resolvedSchema(): Schema? =
        when (this) {
            is SchemaInterface.SchemaInline -> schema
            is SchemaInterface.SchemaReference -> reference.model as? Schema
            else -> null
        }

    private fun String.toParameterName(): String {
        return MapperUtil.toCamelCase(this)
    }

    private fun AnalyzedChannel.shouldGenerateClient(): Boolean =
        (isConsumer && generateConsumers) || (isProducer && generateProducers)

    private fun KafkaPayload.methodName(
        singleName: String,
        multiplePrefix: String,
        messageCount: Int,
    ): String =
        if (messageCount == 1) {
            singleName
        } else {
            "$multiplePrefix$messageName"
        }

    private fun String.toKotlinStringLiteral(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("${'$'}", "\\${'$'}")
}
