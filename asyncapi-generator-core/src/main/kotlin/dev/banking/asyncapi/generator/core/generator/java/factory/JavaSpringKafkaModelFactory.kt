package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.java.mapper.ConstraintMapper
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.kafka.spring.JakartaValidationImportResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHandlerPayloadTypeValidator
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHeaderProperty
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHeaderPropertyFactory
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaKeyContract
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaKeyContractResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayload
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaTopicAddress
import dev.banking.asyncapi.generator.core.generator.kafka.spring.NativeKafkaPayloadResolver
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class JavaSpringKafkaModelFactory(
    private val clientPackage: String,
    private val modelPackage: String,
    private val generateHeaders: Boolean = true,
    private val generateProducers: Boolean = true,
    private val generateConsumers: Boolean = true,
    private val topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    private val validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    private val constraintMapper = ConstraintMapper()

    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        if (!channel.shouldGenerateClient()) {
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

        if (channel.isConsumer && generateConsumers) {
            KafkaHandlerPayloadTypeValidator.validate(
                channelName = channel.channelName,
                payloads = payloads,
            )
            val consumerName = "${baseName}Consumer"
            val methods =
                payloads.map { payload ->
                    val headerProperties =
                        payload.headerProperties.mapIndexed { index, header ->
                            GeneratorItem.HeaderProperty(
                                wireName = header.wireName,
                                parameterName = header.parameterName,
                                typeName = "String",
                                description = header.consumerDescription(),
                                required = header.required,
                                requiredAnnotation = if (header.required) "@NotNull" else null,
                                nullableAnnotation = if (header.required) null else "@Nullable",
                                parameterSuffix = if (index == payload.headerProperties.lastIndex) "" else ",",
                            )
                        }
                    GeneratorItem.ConsumerMethod(
                        messageName = payload.messageName,
                        methodName = payload.methodName("listen"),
                        payloadType = payload.payloadType,
                        payloadDescription =
                            DocumentationUtils.toJavaDocLines(payload.payloadDescription)
                                .ifEmpty { listOf("Message payload.") },
                        keyParameter =
                            keyContracts.getValue(payload)?.toJavaKeyParameter(
                                parameterName = "receivedKey",
                                consumer = true,
                                hasFollowingParameters = headerProperties.isNotEmpty(),
                            ),
                        headerType = payload.headerTypeName,
                        headerProperties = headerProperties,
                        payloadParameterAnnotation = validationAnnotations.payloadParameter?.simpleName,
                        requiredHeaderAnnotation = "NotNull",
                        handlerAnnotation = "KafkaHandler".takeIf { payloads.size > 1 },
                    )
                }
            val keyAnnotations =
                methods.flatMap { method -> method.keyParameter?.annotations.orEmpty() }
            val imports =
                (
                    payloads.mapNotNull { payload -> payload.importName } +
                        keyContracts.values.mapNotNull { keyContract -> keyContract?.importName } +
                        JakartaValidationImportResolver.resolve(keyAnnotations) +
                        "jakarta.validation.constraints.NotNull" +
                        "org.springframework.kafka.support.KafkaHeaders" +
                        "org.springframework.messaging.handler.annotation.Header" +
                        "org.springframework.messaging.handler.annotation.Payload" +
                        listOfNotNull(
                            "org.springframework.kafka.annotation.KafkaHandler".takeIf {
                                methods.any { method -> method.handlerAnnotation != null }
                            },
                            "org.springframework.lang.Nullable".takeIf {
                                methods.any { method ->
                                    method.keyParameter?.required == false ||
                                        method.headerProperties.any { header -> !header.required }
                                }
                            },
                            validationAnnotations.clientContract?.value,
                            validationAnnotations.payloadParameter?.value,
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

        if (channel.isProducer && generateProducers) {
            val sendMethods =
                payloads.map { payload ->
                    val headerProperties =
                        payload.headerProperties.mapIndexed { index, header ->
                            GeneratorItem.HeaderProperty(
                                wireName = header.wireName,
                                parameterName = header.parameterName,
                                typeName = "String",
                                description = header.producerDescription(),
                                required = header.required,
                                requiredAnnotation = if (header.required) "@NotNull" else null,
                                nullableAnnotation = if (header.required) null else "@Nullable",
                                parameterSuffix = if (index == payload.headerProperties.lastIndex) "" else ",",
                                bindingAnnotation =
                                    "Header(" +
                                        "name = \"${header.wireName.toJavaStringLiteral()}\", " +
                                        "required = ${header.required}" +
                                        ")",
                            )
                        }
                    GeneratorItem.SendMethod(
                        methodName = payload.methodName("send"),
                        payloadType = payload.payloadType,
                        payloadDescription =
                            DocumentationUtils.toJavaDocLines(payload.payloadDescription)
                                .ifEmpty { listOf("Message payload.") },
                        payloadBindingAnnotation = "Payload",
                        keyParameter =
                            keyContracts.getValue(payload)?.toJavaKeyParameter(
                                parameterName = "messageKey",
                                consumer = false,
                                hasFollowingParameters = headerProperties.isNotEmpty(),
                            ),
                        headerType = payload.headerTypeName,
                        headerProperties = headerProperties,
                        payloadParameterAnnotation = validationAnnotations.payloadParameter?.simpleName,
                    )
                }
            val keyAnnotations =
                sendMethods.flatMap { method -> method.keyParameter?.annotations.orEmpty() }
            val imports =
                (
                    payloads.mapNotNull { payload -> payload.importName } +
                        keyContracts.values.mapNotNull { keyContract -> keyContract?.importName } +
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
                                        method.headerProperties.any { header -> !header.required }
                                }
                            },
                            "jakarta.validation.constraints.NotNull".takeIf {
                                sendMethods.any { method ->
                                    method.headerProperties.any { header -> header.required }
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
                            "Producer contract for publishing messages to the {@code ${channel.topic}} topic.",
                        ) +
                            DocumentationUtils.toJavaDocLines(
                                "The contract exposes message payloads and contract-defined headers as method parameters.",
                            ) +
                            DocumentationUtils.toJavaDocLines(
                                "Messages with a bindings.kafka.key schema also expose a typed Kafka record key.",
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
            keySchema = msg.keySchema,
            importName =
                if (isPrimitive(type)) {
                    null
                } else {
                    "$modelPackage.$type"
                },
            headerTypeName = headers?.typeName,
            headerImportName = headers?.typeName?.let { "$clientPackage.header.$it" },
            headerProperties = KafkaHeaderPropertyFactory.create(headers),
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

    private fun KafkaPayload.withHeaders(headers: AnalyzedMessageHeaders?): KafkaPayload =
        if (generateHeaders) {
            copy(
                headerTypeName = headers?.typeName,
                headerImportName = headers?.typeName?.let { "$clientPackage.header.$it" },
                headerProperties = KafkaHeaderPropertyFactory.create(headers),
            )
        } else {
            this
        }

    private fun KafkaHeaderProperty.consumerDescription(): List<String> =
        DocumentationUtils.toJavaDocLines(
            buildString {
                append("Value bound from the {@code $wireName} Kafka message header.")
                description?.let { value -> append(" $value") }
            },
        )

    private fun KafkaHeaderProperty.producerDescription(): List<String> =
        DocumentationUtils.toJavaDocLines(
            buildString {
                append("Value for the {@code $wireName} Kafka message header. ")
                append("Implementations must add this value to the outgoing Kafka record.")
                description?.let { value -> append(" $value") }
            },
        )

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

    private fun AnalyzedChannel.shouldGenerateClient(): Boolean =
        (isConsumer && generateConsumers) || (isProducer && generateProducers)

    private fun KafkaPayload.methodName(
        prefix: String,
    ): String = "$prefix$messageName"

    private fun String.toJavaStringLiteral(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
}
