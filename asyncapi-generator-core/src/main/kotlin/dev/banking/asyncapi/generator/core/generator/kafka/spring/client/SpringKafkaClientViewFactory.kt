package dev.banking.asyncapi.generator.core.generator.kafka.spring.client

import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.kafka.spring.JakartaValidationImportResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.NativeKafkaPayloadResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaChannelContract
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaChannelContractFactory
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedChannel

/**
 * Builds the consumer and producer views of a channel's Spring Kafka client in one source language.
 *
 * @param syntax How the source language writes the client.
 * @param clientPackage Package of the generated clients.
 * @param modelPackage Package of the generated models the clients use.
 * @param generateProducers Whether producer interfaces are generated.
 * @param additionalPayloadTypes Serialized payload types producers also send.
 * @param generateConsumers Whether consumer interfaces are generated.
 * @param topicParameterProperties Spring properties that supply channel address parameters.
 * @param validationAnnotations Annotations added to client contracts and payload parameters.
 * @param nativeKafkaPayloadResolver Resolves payloads in native schema formats.
 */
internal class SpringKafkaClientViewFactory(
    private val syntax: SpringKafkaClientSyntax,
    private val clientPackage: String,
    private val modelPackage: String,
    private val generateProducers: Boolean = true,
    additionalPayloadTypes: Set<AdditionalProducerPayloadType> = emptySet(),
    private val generateConsumers: Boolean = true,
    topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    private val validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    private val methodViews = SpringKafkaMethodViews(syntax, validationAnnotations)
    private val channelContractFactory =
        SpringKafkaChannelContractFactory(
            additionalPayloadTypes = additionalPayloadTypes,
            topicParameterProperties = topicParameterProperties,
            nativeKafkaPayloadResolver = nativeKafkaPayloadResolver,
        )

    /** The client views of [channel]: its consumer, then its producer, as configured. */
    fun create(channel: AnalyzedChannel): List<SpringKafkaClientView> {
        if (!generateConsumers && !generateProducers) return emptyList()

        val channelContract = channelContractFactory.create(channel)
        return buildList {
            if (generateConsumers) add(createConsumer(channelContract))
            if (generateProducers) add(createProducer(channelContract))
        }
    }

    private fun createConsumer(channel: SpringKafkaChannelContract): SpringKafkaClientView.Consumer {
        val methods = channel.messages.map(methodViews::consumerMethod)
        return SpringKafkaClientView.Consumer(
            name = "${channel.baseName}Consumer",
            packageName = "$clientPackage.consumer",
            description =
                syntax.docLines(
                    "Defines the Spring Kafka consumer contract for messages received from the " +
                        "${syntax.code(channel.topic)} AsyncAPI channel.",
                ),
            topicAddressConstantName = channel.topicAddress.constantName,
            topicAddress = syntax.stringLiteral(channel.topicAddress.propertyPlaceholderValue),
            methods = methods,
            clientContractAnnotation = validationAnnotations.clientContract?.simpleName,
            imports = consumerImports(channel, methods),
        )
    }

    private fun createProducer(channel: SpringKafkaChannelContract): SpringKafkaClientView.Producer {
        val methods = channel.messages.flatMap(methodViews::sendMethods)
        return SpringKafkaClientView.Producer(
            name = "${channel.baseName}Producer",
            packageName = "$clientPackage.producer",
            description =
                syntax.docLines(
                    "Defines the Spring Kafka producer contract for messages published to the " +
                        "${syntax.code(channel.topic)} AsyncAPI channel.",
                ),
            topicAddressConstantName = channel.topicAddress.constantName,
            topicAddress = syntax.stringLiteral(channel.topicAddress.propertyPlaceholderValue),
            sendMethods = methods,
            anyType = syntax.anyType,
            clientContractAnnotation = validationAnnotations.clientContract?.simpleName,
            imports = producerImports(channel, methods),
        )
    }

    private fun consumerImports(
        channel: SpringKafkaChannelContract,
        methods: List<SpringKafkaClientView.ConsumerMethod>,
    ): List<String> =
        (
            channel.contractImports() +
                "org.springframework.kafka.support.KafkaHeaders" +
                "org.springframework.messaging.handler.annotation.Header" +
                listOfNotNull(
                    "org.springframework.messaging.handler.annotation.Payload".takeIf {
                        methods.any { method -> method.hasPayload }
                    },
                    NULLABLE_IMPORT.takeIf {
                        methods.any { method ->
                            method.keyParameter.isNullable() ||
                                method.headerProperties.anyNullable()
                        }
                    },
                    validationAnnotations.payloadParameter?.value?.takeIf {
                        methods.any { method -> method.payloadParameterAnnotation != null }
                    },
                ) +
                JakartaValidationImportResolver.resolve(
                    methods.flatMap { method ->
                        method.keyParameter.annotations() + listOfNotNull(method.requiredHeaderAnnotation)
                    },
                )
        ).withClientContractImport()

    private fun producerImports(
        channel: SpringKafkaChannelContract,
        methods: List<SpringKafkaClientView.SendMethod>,
    ): List<String> {
        val headers = methods.flatMap { method -> method.headerProperties }
        return (
            channel.contractImports() +
                "java.util.concurrent.CompletableFuture" +
                "org.apache.kafka.clients.producer.RecordMetadata" +
                listOfNotNull(
                    "org.springframework.messaging.handler.annotation.Payload".takeIf {
                        methods.any { method -> method.payloadBindingAnnotation != null }
                    },
                    "org.springframework.messaging.handler.annotation.Header".takeIf {
                        headers.any { header -> header.bindingAnnotation != null }
                    },
                    NULLABLE_IMPORT.takeIf {
                        methods.any { method -> method.keyParameter.isNullable() } || headers.anyNullable()
                    },
                    validationAnnotations.payloadParameter?.value?.takeIf {
                        methods.any { method -> method.payloadParameterAnnotation != null }
                    },
                ) +
                JakartaValidationImportResolver.resolve(
                    methods.flatMap { method -> method.keyParameter.annotations() } +
                        headers.mapNotNull { header -> header.requiredAnnotation },
                )
        ).withClientContractImport()
    }

    private fun List<String>.withClientContractImport(): List<String> =
        (this + listOfNotNull(validationAnnotations.clientContract?.value)).distinct().sorted()

    private fun SpringKafkaClientView.KeyParameter?.annotations(): List<String> = this?.annotations.orEmpty()

    private fun SpringKafkaClientView.KeyParameter?.isNullable(): Boolean =
        this != null && syntax.nullabilityAnnotation(nullable = true) in annotations

    private fun List<SpringKafkaClientView.HeaderProperty>.anyNullable(): Boolean =
        any { header -> header.nullableAnnotation != null }

    private fun SpringKafkaChannelContract.contractImports(): List<String> =
        messages.flatMap { message ->
            message.payload.type
                ?.let { type -> syntax.imports(type, modelPackage) }
                .orEmpty() +
                message.keyContract
                    ?.let { key -> syntax.imports(key.type, modelPackage) }
                    .orEmpty() +
                message.payload.headerProperties.flatMap { header -> syntax.imports(header.type, modelPackage) }
        }

    private companion object {
        const val NULLABLE_IMPORT = "org.springframework.lang.Nullable"
    }
}
