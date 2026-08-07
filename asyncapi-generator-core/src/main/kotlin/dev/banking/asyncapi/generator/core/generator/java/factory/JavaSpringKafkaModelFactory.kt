package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.model.ConstraintAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.kafka.spring.JakartaValidationImportResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHeaderProperty
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaKeyContract
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaKeyContractResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayload
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayloadFactory
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaTopicAddress
import dev.banking.asyncapi.generator.core.generator.kafka.spring.NativeKafkaPayloadResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.inCanonicalOrder
import dev.banking.asyncapi.generator.core.generator.kafka.spring.methodSuffix
import dev.banking.asyncapi.generator.core.generator.kafka.spring.serializedPayloadDescription
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil

class JavaSpringKafkaModelFactory(
    private val clientPackage: String,
    private val modelPackage: String,
    private val generateProducers: Boolean = true,
    private val additionalPayloadTypes: Set<AdditionalProducerPayloadType> = emptySet(),
    private val generateConsumers: Boolean = true,
    private val topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    private val validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    private val constraintMapper = ConstraintAnnotationMapper(SourceLanguage.JAVA)
    private val payloadFactory = KafkaPayloadFactory(modelPackage, nativeKafkaPayloadResolver)

    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        if (!generateConsumers && !generateProducers) {
            return emptyList()
        }

        val items = mutableListOf<GeneratorItem>()
        val baseName = MapperUtil.toPascalCase(channel.channelName)
        val producerPackage = "$clientPackage.producer"
        val consumerPackage = "$clientPackage.consumer"
        val payloads = payloadFactory.create(channel)
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
                        payload.headerProperties.mapIndexed { index, header ->
                            GeneratorItem.HeaderProperty(
                                wireName = header.wireName,
                                parameterName = header.parameterName,
                                typeName = header.javaTypeName,
                                description = header.parameterDescription(),
                                required = header.required,
                                requiredAnnotation = if (header.nullable) null else "@NotNull",
                                nullableAnnotation = if (header.nullable) "@Nullable" else null,
                                parameterSuffix = if (index == payload.headerProperties.lastIndex) "" else ",",
                            )
                        }
                    GeneratorItem.ConsumerMethod(
                        messageName = payload.messageName,
                        methodName = payload.methodName("listen"),
                        payloadType = payload.javaTypeName,
                        payloadDescription =
                            if (payload.hasPayload) {
                                DocumentationUtils.toJavaDocLines(payload.payloadDescription)
                                    .ifEmpty { listOf("Message payload.") }
                            } else {
                                emptyList()
                            },
                        keyParameter =
                            keyContracts.getValue(payload)?.toJavaKeyParameter(
                                parameterName = "receivedKey",
                                consumer = true,
                                hasFollowingParameters = headerProperties.isNotEmpty(),
                            ),
                        headerProperties = headerProperties,
                        payloadParameterAnnotation =
                            validationAnnotations.payloadParameter
                                ?.simpleName
                                ?.takeIf { payload.hasPayload },
                        requiredHeaderAnnotation = "NotNull",
                    )
                }
            val keyAnnotations =
                methods.flatMap { method -> method.keyParameter?.annotations.orEmpty() }
            val imports =
                (
                    payloads.mapNotNull { payload -> payload.javaImportName } +
                        keyContracts.values.mapNotNull { keyContract -> keyContract?.importName } +
                        payloads.flatMap { payload ->
                            payload.headerProperties.mapNotNull { header -> header.importName }
                        } +
                        JakartaValidationImportResolver.resolve(keyAnnotations) +
                        "jakarta.validation.constraints.NotNull" +
                        "org.springframework.kafka.support.KafkaHeaders" +
                        "org.springframework.messaging.handler.annotation.Header" +
                        listOfNotNull(
                            "org.springframework.messaging.handler.annotation.Payload".takeIf {
                                methods.any { method -> method.hasPayload }
                            },
                            "org.springframework.lang.Nullable".takeIf {
                                methods.any { method ->
                                    method.keyParameter?.required == false ||
                                        method.headerProperties.any { header -> header.nullableAnnotation != null }
                                }
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
                        DocumentationUtils.toJavaDocLines(
                            "Defines the Spring Kafka consumer contract for messages received from the " +
                                "{@code ${channel.topic}} AsyncAPI channel.",
                        ),
                    topicAddressConstantName = topicAddress.constantName,
                    topicAddress = topicAddress.propertyPlaceholderValue.toJavaStringLiteral(),
                    methods = methods,
                    clientContractAnnotation = validationAnnotations.clientContract?.simpleName,
                    imports = imports,
                ),
            )
        }

        if (generateProducers) {
            val sendMethods =
                payloads.flatMap { payload ->
                    val headerProperties =
                        payload.headerProperties.mapIndexed { index, header ->
                            GeneratorItem.HeaderProperty(
                                wireName = header.wireName,
                                parameterName = header.parameterName,
                                typeName = header.javaTypeName,
                                description = header.parameterDescription(),
                                required = header.required,
                                requiredAnnotation = if (header.nullable) null else "@NotNull",
                                nullableAnnotation = if (header.nullable) "@Nullable" else null,
                                parameterSuffix = if (index == payload.headerProperties.lastIndex) "" else ",",
                                bindingAnnotation =
                                    "Header(" +
                                        "name = \"${header.wireName.toJavaStringLiteral()}\", " +
                                        "required = ${header.required}" +
                                        ")",
                            )
                        }
                    val configuredAdditionalTypes =
                        if (payload.hasPayload) {
                            additionalPayloadTypes.inCanonicalOrder()
                        } else {
                            emptyList()
                        }
                    val methodPayloadTypes: List<AdditionalProducerPayloadType?> =
                        listOf(null) + configuredAdditionalTypes

                    methodPayloadTypes.map { additionalPayloadType ->
                        GeneratorItem.SendMethod(
                            messageName = payload.messageName,
                            methodName =
                                payload.methodName("send") + additionalPayloadType?.methodSuffix.orEmpty(),
                            payloadType = payload.javaProducerPayloadType(additionalPayloadType),
                            payloadDescription = payload.producerPayloadDescription(additionalPayloadType),
                            payloadBindingAnnotation = "Payload".takeIf { payload.hasPayload },
                            keyParameter =
                                keyContracts.getValue(payload)?.toJavaKeyParameter(
                                    parameterName = "messageKey",
                                    consumer = false,
                                    hasFollowingParameters = headerProperties.isNotEmpty(),
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
                    payloads.mapNotNull { payload -> payload.javaImportName } +
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
                            "org.springframework.lang.Nullable".takeIf {
                                sendMethods.any { method ->
                                    method.keyParameter?.required == false ||
                                        method.headerProperties.any { header -> header.nullableAnnotation != null }
                                }
                            },
                            "jakarta.validation.constraints.NotNull".takeIf {
                                sendMethods.any { method ->
                                    method.headerProperties.any { header -> header.requiredAnnotation != null }
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
                        DocumentationUtils.toJavaDocLines(
                            "Defines the Spring Kafka producer contract for messages published to the " +
                                "{@code ${channel.topic}} AsyncAPI channel.",
                        ),
                    topicAddressConstantName = topicAddress.constantName,
                    topicAddress = topicAddress.propertyPlaceholderValue.toJavaStringLiteral(),
                    sendMethods = sendMethods,
                    clientContractAnnotation = validationAnnotations.clientContract?.simpleName,
                    imports = imports,
                ),
            )
        }

        return items
    }

    private fun KafkaPayload.javaProducerPayloadType(
        additionalPayloadType: AdditionalProducerPayloadType?,
    ): String? =
        when (additionalPayloadType) {
            AdditionalProducerPayloadType.BYTE_ARRAY -> "byte[]"
            AdditionalProducerPayloadType.STRING -> "String"
            null -> javaTypeName
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
                    DocumentationUtils.toJavaDocLines(payloadDescription)
                        .ifEmpty { listOf("Message payload.") }
                } else {
                    emptyList()
                }
        }

    private fun KafkaHeaderProperty.parameterDescription(): List<String> =
        description
            ?.takeIf { value -> value.isNotBlank() }
            ?.let(DocumentationUtils::toJavaDocLines)
            ?: listOf("Value of the {@code $wireName} Kafka message header.")

    private fun KafkaKeyContract.toJavaKeyParameter(
        parameterName: String,
        consumer: Boolean,
        hasFollowingParameters: Boolean,
    ): GeneratorItem.KeyParameter {
        val annotations =
            constraintMapper.buildAnnotations(schema) +
                listOfNotNull(
                    validationAnnotations.payloadParameter
                        ?.simpleName
                        ?.takeIf { isModel }
                        ?.let { "@$it" },
                ) +
                if (nullable) "@Nullable" else "@NotNull"
        val defaultDescription =
            if (consumer && nullable) {
                "Kafka record key, or {@code null} when the record has no key."
            } else {
                "Kafka record key."
            }
        return GeneratorItem.KeyParameter(
            parameterName = parameterName,
            typeName = javaTypeName,
            description =
                DocumentationUtils.toJavaDocLines(schema.description)
                    .ifEmpty { listOf(defaultDescription) },
            required = !nullable,
            annotations = annotations,
            parameterSuffix = if (hasFollowingParameters) "," else "",
        )
    }

    private fun KafkaPayload.methodName(
        prefix: String,
    ): String = "$prefix$messageName"

    private fun String.toJavaStringLiteral(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
}
