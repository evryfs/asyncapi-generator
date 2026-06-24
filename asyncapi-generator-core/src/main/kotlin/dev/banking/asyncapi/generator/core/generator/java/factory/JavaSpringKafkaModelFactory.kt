package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHeaderProperty
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayload
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
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    fun create(channel: AnalyzedChannel): List<GeneratorItem> {
        val items = mutableListOf<GeneratorItem>()
        val baseName = MapperUtil.toPascalCase(channel.channelName)
        val producerPackage = "$clientPackage.producer"
        val consumerPackage = "$clientPackage.consumer"
        val payloads = channel.payloads()

        if (channel.isConsumer && generateConsumers) {
            val consumerName = "${baseName}Consumer"
            val imports =
                (
                    payloads.mapNotNull { payload -> payload.importName } +
                        "jakarta.validation.Valid" +
                        "jakarta.validation.constraints.NotNull" +
                        "org.springframework.lang.Nullable" +
                        "org.springframework.validation.annotation.Validated"
                )
                    .distinct()
                    .sorted()
            val methods =
                payloads.map { payload ->
                    GeneratorItem.ConsumerMethod(
                        methodName = "on${payload.messageName}",
                        payloadType = payload.payloadType,
                        payloadDescription =
                            DocumentationUtils.toJavaDocLines(payload.payloadDescription)
                                .ifEmpty { listOf("Message payload.") },
                        keyDescription = listOf("Kafka record key."),
                        headerType = payload.headerTypeName,
                        headerProperties =
                            payload.headerProperties.mapIndexed { index, header ->
                                GeneratorItem.HeaderProperty(
                                    name = header.name,
                                    accessorName = header.accessorName,
                                    parameterName = header.accessorName,
                                    typeName = "String",
                                    description =
                                        DocumentationUtils.toJavaDocLines(header.description)
                                            .ifEmpty { listOf("Kafka message header.") },
                                    required = header.required,
                                    nullableAnnotation = if (header.required) null else "@Nullable",
                                    parameterSuffix = if (index == payload.headerProperties.lastIndex) "" else ",",
                                )
                            },
                    )
                }
            items.add(
                GeneratorItem.KafkaConsumerInterface(
                    name = consumerName,
                    packageName = consumerPackage,
                    description =
                        DocumentationUtils.toJavaDocLines(
                            "Consumer contract for handling messages from the {@code ${channel.topic}} topic.",
                        ) +
                            DocumentationUtils.toJavaDocLines(
                                "The contract exposes the Kafka record key, message payload, and " +
                                    "contract-defined headers as method parameters.",
                            ),
                    methods = methods,
                    imports = imports,
                ),
            )
        }

        if (channel.isProducer && generateProducers) {
            payloads.forEach { payload ->
                val imports =
                    (
                        listOfNotNull(payload.importName) +
                            "jakarta.validation.Valid" +
                            "jakarta.validation.constraints.NotNull" +
                            "org.springframework.validation.annotation.Validated" +
                            listOfNotNull(
                                "org.springframework.lang.Nullable".takeIf {
                                    payload.headerProperties.any { header -> !header.required }
                                },
                            )
                    )
                        .distinct()
                        .sorted()
                val sendMethod =
                    GeneratorItem.SendMethod(
                        methodName = "send${payload.messageName}",
                        payloadType = payload.payloadType,
                        payloadDescription =
                            DocumentationUtils.toJavaDocLines(payload.payloadDescription)
                                .ifEmpty { listOf("Message payload.") },
                        keyDescription = listOf("Kafka record key."),
                        headerType = payload.headerTypeName,
                        headerProperties =
                            payload.headerProperties.mapIndexed { index, header ->
                                GeneratorItem.HeaderProperty(
                                    name = header.name,
                                    accessorName = header.accessorName,
                                    parameterName = header.accessorName,
                                    typeName = "String",
                                    description =
                                        DocumentationUtils.toJavaDocLines(header.description)
                                            .ifEmpty { listOf("Kafka message header.") },
                                    required = header.required,
                                    nullableAnnotation = if (header.required) null else "@Nullable",
                                    parameterSuffix = if (index == payload.headerProperties.lastIndex) "" else ",",
                                )
                            },
                    )
                val producerName = "${baseName}Producer${payload.messageName}"
                items.add(
                    GeneratorItem.KafkaProducerClass(
                        name = producerName,
                        packageName = producerPackage,
                        description =
                            DocumentationUtils.toJavaDocLines(
                                "Producer contract for publishing messages to the {@code ${channel.topic}} topic.",
                            ) +
                                DocumentationUtils.toJavaDocLines(
                                    "The contract exposes the Kafka record key, message payload, and " +
                                        "contract-defined headers as method parameters.",
                                ),
                        topic = channel.topic,
                        sendMethods = listOf(sendMethod),
                        kafkaValueType = payload.payloadType,
                        imports = imports,
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

    private fun SchemaInterface.description(): String? = resolvedSchema()?.description

    private fun SchemaInterface.resolvedSchema(): Schema? =
        when (this) {
            is SchemaInterface.SchemaInline -> schema
            is SchemaInterface.SchemaReference -> reference.model as? Schema
            else -> null
        }

    private fun String.toParameterName(): String {
        val pascalCase = MapperUtil.toPascalCase(this)
        return pascalCase.replaceFirstChar { it.lowercase() }
    }
}
