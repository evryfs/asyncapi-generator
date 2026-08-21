package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.model.ConstraintAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.SourceLiteralEscaper
import dev.banking.asyncapi.generator.core.generator.kafka.spring.JakartaValidationImportResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHeaderProperty
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaKeyContract
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayload
import dev.banking.asyncapi.generator.core.generator.kafka.spring.NativeKafkaPayloadResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaChannelContract
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaChannelContractFactory
import dev.banking.asyncapi.generator.core.generator.kafka.spring.serializedPayloadDescription
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils

class JavaSpringKafkaModelFactory(
    private val clientPackage: String,
    modelPackage: String,
    private val generateProducers: Boolean = true,
    additionalPayloadTypes: Set<AdditionalProducerPayloadType> = emptySet(),
    private val generateConsumers: Boolean = true,
    topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    private val validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    private val constraintMapper = ConstraintAnnotationMapper(SourceLanguage.JAVA)
    private val channelContractFactory =
        SpringKafkaChannelContractFactory(
            modelPackage = modelPackage,
            additionalPayloadTypes = additionalPayloadTypes,
            topicParameterProperties = topicParameterProperties,
            nativeKafkaPayloadResolver = nativeKafkaPayloadResolver,
        )

    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        if (!generateConsumers && !generateProducers) return emptyList()

        val channelContract = channelContractFactory.create(channel)
        return buildList {
            if (generateConsumers) add(createConsumer(channelContract))
            if (generateProducers) add(createProducer(channelContract))
        }
    }

    private fun createConsumer(channel: SpringKafkaChannelContract): GeneratorItem.KafkaConsumerInterface {
        val methods =
            channel.messages.map { message ->
                val payload = message.payload
                val headers = payload.javaHeaders(producer = false)
                GeneratorItem.ConsumerMethod(
                    messageName = payload.messageName,
                    methodName = message.consumerMethodName,
                    payloadType = payload.javaTypeName,
                    payloadDescription = payload.contractPayloadDescription(),
                    keyParameter =
                        message.keyContract?.toJavaKeyParameter(
                            parameterName = "receivedKey",
                            consumer = true,
                            hasFollowingParameters = headers.isNotEmpty(),
                        ),
                    headerProperties = headers,
                    payloadParameterAnnotation =
                        validationAnnotations.payloadParameter?.simpleName?.takeIf { payload.hasPayload },
                    requiredHeaderAnnotation = "NotNull",
                )
            }
        val keyAnnotations = methods.flatMap { method -> method.keyParameter?.annotations.orEmpty() }
        val imports =
            (
                channel.contractImports() +
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
            ).distinct().sorted()

        return GeneratorItem.KafkaConsumerInterface(
            name = "${channel.baseName}Consumer",
            packageName = "$clientPackage.consumer",
            description =
                DocumentationUtils.toJavaDocLines(
                    "Defines the Spring Kafka consumer contract for messages received from the " +
                        "{@code ${channel.topic}} AsyncAPI channel.",
                ),
            topicAddressConstantName = channel.topicAddress.constantName,
            topicAddress = channel.topicAddress.propertyPlaceholderValue.toJavaStringLiteral(),
            methods = methods,
            clientContractAnnotation = validationAnnotations.clientContract?.simpleName,
            imports = imports,
        )
    }

    private fun createProducer(channel: SpringKafkaChannelContract): GeneratorItem.KafkaProducerClass {
        val methods =
            channel.messages.flatMap { message ->
                val payload = message.payload
                val headers = payload.javaHeaders(producer = true)
                message.producerMethods.map { producerMethod ->
                    val additionalPayloadType = producerMethod.additionalPayloadType
                    GeneratorItem.SendMethod(
                        messageName = payload.messageName,
                        methodName = producerMethod.methodName,
                        payloadType = payload.javaProducerPayloadType(additionalPayloadType),
                        payloadDescription = payload.producerPayloadDescription(additionalPayloadType),
                        payloadBindingAnnotation = "Payload".takeIf { payload.hasPayload },
                        keyParameter =
                            message.keyContract?.toJavaKeyParameter(
                                parameterName = "messageKey",
                                consumer = false,
                                hasFollowingParameters = headers.isNotEmpty(),
                            ),
                        headerProperties = headers,
                        payloadParameterAnnotation =
                            validationAnnotations.payloadParameter
                                ?.simpleName
                                ?.takeIf { payload.hasPayload && additionalPayloadType == null },
                        additionalPayloadType = additionalPayloadType,
                    )
                }
            }
        val keyAnnotations = methods.flatMap { method -> method.keyParameter?.annotations.orEmpty() }
        val imports =
            (
                channel.contractImports() +
                    JakartaValidationImportResolver.resolve(keyAnnotations) +
                    "java.util.concurrent.CompletableFuture" +
                    "org.apache.kafka.clients.producer.RecordMetadata" +
                    listOfNotNull(
                        "org.springframework.messaging.handler.annotation.Payload".takeIf {
                            methods.any { method -> method.payloadBindingAnnotation != null }
                        },
                        "org.springframework.messaging.handler.annotation.Header".takeIf {
                            methods.any { method ->
                                method.headerProperties.any { header -> header.bindingAnnotation != null }
                            }
                        },
                        "org.springframework.lang.Nullable".takeIf {
                            methods.any { method ->
                                method.keyParameter?.required == false ||
                                    method.headerProperties.any { header -> header.nullableAnnotation != null }
                            }
                        },
                        "jakarta.validation.constraints.NotNull".takeIf {
                            methods.any { method ->
                                method.headerProperties.any { header -> header.requiredAnnotation != null }
                            }
                        },
                        validationAnnotations.clientContract?.value,
                        validationAnnotations.payloadParameter?.value?.takeIf {
                            methods.any { method -> method.payloadParameterAnnotation != null }
                        },
                    )
            ).distinct().sorted()

        return GeneratorItem.KafkaProducerClass(
            name = "${channel.baseName}Producer",
            packageName = "$clientPackage.producer",
            description =
                DocumentationUtils.toJavaDocLines(
                    "Defines the Spring Kafka producer contract for messages published to the " +
                        "{@code ${channel.topic}} AsyncAPI channel.",
                ),
            topicAddressConstantName = channel.topicAddress.constantName,
            topicAddress = channel.topicAddress.propertyPlaceholderValue.toJavaStringLiteral(),
            sendMethods = methods,
            clientContractAnnotation = validationAnnotations.clientContract?.simpleName,
            imports = imports,
        )
    }

    private fun KafkaPayload.javaProducerPayloadType(
        additionalPayloadType: AdditionalProducerPayloadType?,
    ): String? =
        when (additionalPayloadType) {
            AdditionalProducerPayloadType.BYTE_ARRAY -> "byte[]"
            AdditionalProducerPayloadType.STRING -> "String"
            null -> javaTypeName
        }

    private fun KafkaPayload.contractPayloadDescription(): List<String> =
        if (hasPayload) {
            DocumentationUtils.toJavaDocLines(payloadDescription)
                .ifEmpty { listOf("Message payload.") }
        } else {
            emptyList()
        }

    private fun KafkaPayload.producerPayloadDescription(
        additionalPayloadType: AdditionalProducerPayloadType?,
    ): List<String> =
        when (additionalPayloadType) {
            AdditionalProducerPayloadType.BYTE_ARRAY,
            AdditionalProducerPayloadType.STRING,
            -> additionalPayloadType.serializedPayloadDescription()
            null -> contractPayloadDescription()
        }

    private fun KafkaPayload.javaHeaders(producer: Boolean): List<GeneratorItem.HeaderProperty> =
        headerProperties.mapIndexed { index, header ->
            GeneratorItem.HeaderProperty(
                wireName = header.wireName,
                parameterName = header.parameterName,
                typeName = header.javaTypeName,
                description = header.parameterDescription(),
                required = header.required,
                requiredAnnotation = if (header.nullable) null else "@NotNull",
                nullableAnnotation = if (header.nullable) "@Nullable" else null,
                parameterSuffix = if (index == headerProperties.lastIndex) "" else ",",
                bindingAnnotation =
                    if (producer) {
                        "Header(" +
                            "name = \"${header.wireName.toJavaStringLiteral()}\", " +
                            "required = ${header.required}" +
                            ")"
                    } else {
                        null
                    },
            )
        }

    private fun SpringKafkaChannelContract.contractImports(): List<String> =
        messages.flatMap { message ->
            listOfNotNull(
                message.payload.javaImportName,
                message.keyContract?.importName,
            ) + message.payload.headerProperties.mapNotNull { header -> header.importName }
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

    private fun String.toJavaStringLiteral(): String = SourceLiteralEscaper.forJava(this)
}
