package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.kafka.spring.JakartaValidationImportResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHeaderProperty
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaKeyContract
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayload
import dev.banking.asyncapi.generator.core.generator.kafka.spring.NativeKafkaPayloadResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaChannelContractFactory
import dev.banking.asyncapi.generator.core.generator.kafka.spring.serializedPayloadDescription
import dev.banking.asyncapi.generator.core.generator.model.ConstraintAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils.toKDocLines

class KotlinSpringKafkaModelFactory(
    private val clientPackage: String,
    private val modelPackage: String,
    private val generateProducers: Boolean = true,
    private val additionalPayloadTypes: Set<AdditionalProducerPayloadType> = emptySet(),
    private val generateConsumers: Boolean = true,
    private val topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    private val validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    private val constraintMapper = ConstraintAnnotationMapper(SourceLanguage.KOTLIN)
    private val channelContractFactory =
        SpringKafkaChannelContractFactory(
            modelPackage = modelPackage,
            additionalPayloadTypes = additionalPayloadTypes,
            topicParameterProperties = topicParameterProperties,
            nativeKafkaPayloadResolver = nativeKafkaPayloadResolver,
        )

    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        if (!generateConsumers && !generateProducers) {
            return emptyList()
        }

        val items = mutableListOf<GeneratorItem>()
        val channelContract = channelContractFactory.create(channel)
        val baseName = channelContract.baseName
        val producerPackage = "$clientPackage.producer"
        val consumerPackage = "$clientPackage.consumer"
        val payloads = channelContract.messages.map { message -> message.payload }
        val keyContracts = channelContract.messages.mapNotNull { message -> message.keyContract }
        val topicAddress = channelContract.topicAddress

        if (generateConsumers) {
            val consumerName = "${baseName}Consumer"
            val methods =
                channelContract.messages.map { message ->
                    val payload = message.payload
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
                        methodName = message.consumerMethodName,
                        payloadType = payload.kotlinTypeName,
                        payloadDescription =
                            if (payload.hasPayload) {
                                toKDocLines(payload.payloadDescription)
                                    .ifEmpty { listOf("Message payload.") }
                            } else {
                                emptyList()
                            },
                        keyParameter =
                            message.keyContract?.toKotlinKeyParameter(
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
                    payloads.mapNotNull { payload -> payload.kotlinImportName } +
                        keyContracts.mapNotNull { keyContract -> keyContract.importName } +
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
                                "`${channelContract.topic}` AsyncAPI channel.",
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
                channelContract.messages.flatMap { message ->
                    val payload = message.payload
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
                    message.producerMethods.map { producerMethod ->
                        val additionalPayloadType = producerMethod.additionalPayloadType
                        GeneratorItem.SendMethod(
                            messageName = payload.messageName,
                            methodName = producerMethod.methodName,
                            payloadType = payload.kotlinProducerPayloadType(additionalPayloadType),
                            payloadDescription = payload.producerPayloadDescription(additionalPayloadType),
                            payloadBindingAnnotation = "Payload".takeIf { payload.hasPayload },
                            keyParameter =
                                message.keyContract?.toKotlinKeyParameter(
                                    parameterName = "messageKey",
                                    consumer = false,
                                ),
                            headerProperties = headerProperties,
                            payloadParameterAnnotation =
                                validationAnnotations.payloadParameter
                                    ?.simpleName
                                    ?.takeIf { payload.hasPayload && additionalPayloadType == null },
                            additionalPayloadType = additionalPayloadType,
                        )
                    }
                }
            val keyAnnotations =
                sendMethods.flatMap { method -> method.keyParameter?.annotations.orEmpty() }
            val imports =
                (
                    payloads.mapNotNull { payload -> payload.kotlinImportName } +
                        keyContracts.mapNotNull { keyContract -> keyContract.importName } +
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
                                "`${channelContract.topic}` AsyncAPI channel.",
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

    private fun KafkaPayload.kotlinProducerPayloadType(
        additionalPayloadType: AdditionalProducerPayloadType?,
    ): String? =
        when (additionalPayloadType) {
            AdditionalProducerPayloadType.BYTE_ARRAY -> "ByteArray"
            AdditionalProducerPayloadType.STRING -> "String"
            null -> kotlinTypeName
        }

    private fun KafkaPayload.producerPayloadDescription(
        additionalPayloadType: AdditionalProducerPayloadType?,
    ): List<String> =
        when (additionalPayloadType) {
            AdditionalProducerPayloadType.BYTE_ARRAY,
            AdditionalProducerPayloadType.STRING,
            -> additionalPayloadType.serializedPayloadDescription()
            null ->
                if (hasPayload) {
                    toKDocLines(payloadDescription)
                        .ifEmpty { listOf("Message payload.") }
                } else {
                    emptyList()
                }
        }

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

    private fun String.toKotlinStringLiteral(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("${'$'}", "\\${'$'}")
}
