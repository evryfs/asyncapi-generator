package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.kafka.spring.JakartaValidationImportResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHeaderProperty
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHeaderPropertyFactory
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaKeyContract
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaKeyContractResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayload
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaTopicAddress
import dev.banking.asyncapi.generator.core.generator.kafka.spring.NativeKafkaPayloadResolver
import dev.banking.asyncapi.generator.core.generator.kotlin.mapper.ConstraintMapper
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils.toKDocLines
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema

class KotlinSpringKafkaModelFactory(
    private val clientPackage: String,
    private val modelPackage: String,
    private val generateProducers: Boolean = true,
    private val generateConsumers: Boolean = true,
    private val topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    private val validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    private val constraintMapper = ConstraintMapper()

    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        if (!generateConsumers && !generateProducers) {
            return emptyList()
        }

        val items = mutableListOf<GeneratorItem>()
        val baseName = MapperUtil.toPascalCase(channel.channelName)
        val producerPackage = "$clientPackage.producer"
        val consumerPackage = "$clientPackage.consumer"
        val payloads = channel.payloads()
        val keyContracts =
            payloads.associateWith { payload ->
                KafkaKeyContractResolver.resolve(
                    messageName = payload.messageName,
                    schema = payload.keySchema,
                    modelPackage = modelPackage,
                )
            }
        val topicAddress =
            KafkaTopicAddress.from(
                channelName = channel.channelName,
                value = channel.topic,
                topicParameterProperties = topicParameterProperties,
        )

        if (generateConsumers) {
            val consumerName = "${baseName}Consumer"
            val methods =
                payloads.map { payload ->
                    val headerProperties =
                        payload.headerProperties.map { header ->
                            GeneratorItem.HeaderProperty(
                                wireName = header.wireName,
                                parameterName = header.parameterName,
                                typeName = header.kotlinTypeName + if (header.nullable) "?" else "",
                                description = header.parameterDescription(),
                                required = header.required,
                                defaultValue = if (header.required) null else "null",
                            )
                        }
                    GeneratorItem.ConsumerMethod(
                        messageName = payload.messageName,
                        methodName = payload.methodName("listen"),
                        payloadType = payload.payloadType,
                        payloadDescription =
                            if (payload.hasPayload) {
                                toKDocLines(payload.payloadDescription)
                                    .ifEmpty { listOf("Message payload.") }
                            } else {
                                emptyList()
                            },
                        keyParameter =
                            keyContracts.getValue(payload)?.toKotlinKeyParameter(
                                parameterName = "receivedKey",
                                consumer = true,
                            ),
                        headerProperties = headerProperties,
                        payloadParameterAnnotation =
                            validationAnnotations.payloadParameter
                                ?.simpleName
                                ?.takeIf { payload.hasPayload },
                    )
                }
            val keyAnnotations =
                methods.flatMap { method -> method.keyParameter?.annotations.orEmpty() }
            val imports =
                (
                    payloads.mapNotNull { payload -> payload.importName } +
                        keyContracts.values.mapNotNull { keyContract -> keyContract?.importName } +
                        payloads.flatMap { payload ->
                            payload.headerProperties.mapNotNull { header -> header.importName }
                        } +
                        JakartaValidationImportResolver.resolve(keyAnnotations) +
                        "org.springframework.kafka.support.KafkaHeaders" +
                        "org.springframework.messaging.handler.annotation.Header" +
                        listOfNotNull(
                            "org.springframework.messaging.handler.annotation.Payload".takeIf {
                                methods.any { method -> method.hasPayload }
                            },
                            validationAnnotations.clientContract?.value,
                            validationAnnotations.payloadParameter?.value?.takeIf {
                                methods.any { method -> method.payloadParameterAnnotation != null }
                            },
                        )
                )
                    .distinct()
                    .sorted()
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

        if (generateProducers) {
            val sendMethods =
                payloads.map { payload ->
                    val headerProperties =
                        payload.headerProperties.map { header ->
                            GeneratorItem.HeaderProperty(
                                wireName = header.wireName,
                                parameterName = header.parameterName,
                                typeName = header.kotlinTypeName + if (header.nullable) "?" else "",
                                description = header.parameterDescription(),
                                required = header.required,
                                defaultValue = if (header.required) null else "null",
                                bindingAnnotation =
                                    "Header(" +
                                        "name = \"${header.wireName.toKotlinStringLiteral()}\", " +
                                        "required = ${header.required}" +
                                        ")",
                            )
                        }
                    GeneratorItem.SendMethod(
                        messageName = payload.messageName,
                        methodName = payload.methodName("send"),
                        payloadType = payload.payloadType,
                        payloadDescription =
                            if (payload.hasPayload) {
                                toKDocLines(payload.payloadDescription)
                                    .ifEmpty { listOf("Message payload.") }
                            } else {
                                emptyList()
                            },
                        payloadBindingAnnotation = "Payload".takeIf { payload.hasPayload },
                        keyParameter =
                            keyContracts.getValue(payload)?.toKotlinKeyParameter(
                                parameterName = "messageKey",
                                consumer = false,
                            ),
                        headerProperties = headerProperties,
                        payloadParameterAnnotation =
                            validationAnnotations.payloadParameter
                                ?.simpleName
                                ?.takeIf { payload.hasPayload },
                    )
                }
            val keyAnnotations =
                sendMethods.flatMap { method -> method.keyParameter?.annotations.orEmpty() }
            val imports =
                (
                    payloads.mapNotNull { payload -> payload.importName } +
                        keyContracts.values.mapNotNull { keyContract -> keyContract?.importName } +
                        payloads.flatMap { payload ->
                            payload.headerProperties.mapNotNull { header -> header.importName }
                        } +
                        JakartaValidationImportResolver.resolve(keyAnnotations) +
                        "java.util.concurrent.CompletableFuture" +
                        "org.apache.kafka.clients.producer.RecordMetadata" +
                        listOfNotNull(
                            "org.springframework.messaging.handler.annotation.Payload".takeIf {
                                sendMethods.any { method -> method.payloadBindingAnnotation != null }
                            },
                            "org.springframework.messaging.handler.annotation.Header".takeIf {
                                sendMethods.any { method ->
                                    method.headerProperties.any { header -> header.bindingAnnotation != null }
                                }
                            },
                            validationAnnotations.clientContract?.value,
                            validationAnnotations.payloadParameter?.value?.takeIf {
                                sendMethods.any { method -> method.payloadParameterAnnotation != null }
                            },
                        )
                )
                    .distinct()
                    .sorted()
            items.add(
                GeneratorItem.KafkaProducerClass(
                    name = "${baseName}Producer",
                    packageName = producerPackage,
                    description =
                        toKDocLines(
                            "Defines the Spring Kafka producer contract for messages published to the " +
                                "`${channel.topic}` AsyncAPI channel.",
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
        return KafkaPayload(
            messageName = msg.messageName,
            payloadType = type,
            payloadDescription = msg.schema?.description,
            keySchema = msg.keySchema,
            importName =
                if (type == null || isPrimitive(type)) {
                    null
                } else {
                    "$modelPackage.$type"
                },
            headerProperties = KafkaHeaderPropertyFactory.create(msg.headers, msg.messageName),
        )
    }

    private fun resolvePayloadType(msg: AnalyzedMessage): String? {
        val schema = msg.schema ?: return null
        return when (schema.type.getPrimaryType()) {
            "string" -> "String"
            "integer" -> "Int"
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> msg.payloadTypeName
        }
    }

    private fun isPrimitive(type: String): Boolean = type in setOf("String", "Int", "Long", "Boolean", "java.math.BigDecimal")

    private fun KafkaPayload.withHeaders(headers: AnalyzedMessageHeaders?): KafkaPayload =
        copy(headerProperties = KafkaHeaderPropertyFactory.create(headers, messageName))

    private fun KafkaHeaderProperty.parameterDescription(): List<String> =
        description
            ?.takeIf { value -> value.isNotBlank() }
            ?.let(::toKDocLines)
            ?: listOf("Value of the `$wireName` Kafka message header.")

    private fun KafkaKeyContract.toKotlinKeyParameter(
        parameterName: String,
        consumer: Boolean,
    ): GeneratorItem.KeyParameter {
        val defaultDescription =
            if (consumer && nullable) {
                "Kafka record key, or `null` when the record has no key."
            } else {
                "Kafka record key."
            }
        return GeneratorItem.KeyParameter(
            parameterName = parameterName,
            typeName = kotlinTypeName + if (nullable) "?" else "",
            description =
                toKDocLines(schema.description)
                    .ifEmpty { listOf(defaultDescription) },
            required = !nullable,
            annotations =
                constraintMapper.buildAnnotations(schema)
                    .map { annotation -> annotation.replace("@field:", "@") } +
                    listOfNotNull(
                        validationAnnotations.payloadParameter
                            ?.simpleName
                            ?.takeIf { isModel }
                            ?.let { "@$it" },
                    ),
        )
    }

    private fun KafkaPayload.methodName(
        prefix: String,
    ): String = "$prefix$messageName"

    private fun String.toKotlinStringLiteral(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("${'$'}", "\\${'$'}")
}
