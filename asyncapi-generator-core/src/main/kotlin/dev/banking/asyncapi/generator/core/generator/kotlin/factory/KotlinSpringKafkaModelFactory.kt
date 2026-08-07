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
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaChannelContract
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaChannelContractFactory
import dev.banking.asyncapi.generator.core.generator.kafka.spring.serializedPayloadDescription
import dev.banking.asyncapi.generator.core.generator.model.ConstraintAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils.toKDocLines

class KotlinSpringKafkaModelFactory(
    private val clientPackage: String,
    modelPackage: String,
    private val generateProducers: Boolean = true,
    additionalPayloadTypes: Set<AdditionalProducerPayloadType> = emptySet(),
    private val generateConsumers: Boolean = true,
    topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    private val validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
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
                GeneratorItem.ConsumerMethod(
                    messageName = payload.messageName,
                    methodName = message.consumerMethodName,
                    payloadType = payload.kotlinTypeName,
                    payloadDescription = payload.contractPayloadDescription(),
                    keyParameter =
                        message.keyContract?.toKotlinKeyParameter(
                            parameterName = "receivedKey",
                            consumer = true,
                        ),
                    headerProperties = payload.kotlinHeaders(producer = false),
                    payloadParameterAnnotation =
                        validationAnnotations.payloadParameter?.simpleName?.takeIf { payload.hasPayload },
                )
            }
        val keyAnnotations = methods.flatMap { method -> method.keyParameter?.annotations.orEmpty() }
        val imports =
            (
                channel.contractImports() +
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
            ).distinct().sorted()

        return GeneratorItem.KafkaConsumerInterface(
            name = "${channel.baseName}Consumer",
            packageName = "$clientPackage.consumer",
            description =
                toKDocLines(
                    "Defines the Spring Kafka consumer contract for messages received from the " +
                        "`${channel.topic}` AsyncAPI channel.",
                ),
            topicAddressConstantName = channel.topicAddress.constantName,
            topicAddress = channel.topicAddress.propertyPlaceholderValue.toKotlinStringLiteral(),
            methods = methods,
            clientContractAnnotation = validationAnnotations.clientContract?.simpleName,
            imports = imports,
        )
    }

    private fun createProducer(channel: SpringKafkaChannelContract): GeneratorItem.KafkaProducerClass {
        val methods =
            channel.messages.flatMap { message ->
                val payload = message.payload
                val headers = payload.kotlinHeaders(producer = true)
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
                toKDocLines(
                    "Defines the Spring Kafka producer contract for messages published to the " +
                        "`${channel.topic}` AsyncAPI channel.",
                ),
            topicAddressConstantName = channel.topicAddress.constantName,
            topicAddress = channel.topicAddress.propertyPlaceholderValue.toKotlinStringLiteral(),
            sendMethods = methods,
            clientContractAnnotation = validationAnnotations.clientContract?.simpleName,
            imports = imports,
        )
    }

    private fun KafkaPayload.kotlinProducerPayloadType(
        additionalPayloadType: AdditionalProducerPayloadType?,
    ): String? =
        when (additionalPayloadType) {
            AdditionalProducerPayloadType.BYTE_ARRAY -> "ByteArray"
            AdditionalProducerPayloadType.STRING -> "String"
            null -> kotlinTypeName
        }

    private fun KafkaPayload.contractPayloadDescription(): List<String> =
        if (hasPayload) {
            toKDocLines(payloadDescription)
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

    private fun KafkaPayload.kotlinHeaders(producer: Boolean): List<GeneratorItem.HeaderProperty> =
        headerProperties.map { header ->
            GeneratorItem.HeaderProperty(
                wireName = header.wireName,
                parameterName = header.parameterName,
                typeName = header.kotlinTypeName + if (header.nullable) "?" else "",
                description = header.parameterDescription(),
                required = header.required,
                defaultValue = if (header.required) null else "null",
                bindingAnnotation =
                    if (producer) {
                        "Header(" +
                            "name = \"${header.wireName.toKotlinStringLiteral()}\", " +
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
                message.payload.kotlinImportName,
                message.keyContract?.importName,
            ) + message.payload.headerProperties.mapNotNull { header -> header.importName }
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
