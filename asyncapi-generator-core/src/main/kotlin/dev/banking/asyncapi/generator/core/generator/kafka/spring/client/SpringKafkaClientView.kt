package dev.banking.asyncapi.generator.core.generator.kafka.spring.client

import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType

/**
 * A Spring Kafka client source file as its template renders it, in either source language.
 *
 * Each template reads the members its language needs; the others stay unused.
 */
internal sealed interface SpringKafkaClientView {
    val name: String
    val packageName: String
    val description: List<String>

    /**
     * The consumer interface of a channel.
     *
     * @param name Simple name of the interface.
     * @param packageName Package of the interface.
     * @param description Documentation lines of the interface.
     * @param topicAddressConstantName Name of the topic address constant.
     * @param topicAddress Topic address as a string literal body.
     * @param methods Listener methods, one per message.
     * @param clientContractAnnotation Simple name of the annotation on the interface, if configured.
     * @param imports Imports of the source file.
     */
    data class Consumer(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val topicAddressConstantName: String,
        val topicAddress: String,
        val methods: List<ConsumerMethod>,
        val clientContractAnnotation: String? = null,
        val imports: List<String> = emptyList(),
    ) : SpringKafkaClientView {
        val hasSingleMethod: Boolean get() = methods.size == 1
        val hasMultipleMethods: Boolean get() = methods.size > 1
        val hasPayloadlessMethods: Boolean get() = methods.any { method -> !method.hasPayload }
    }

    /**
     * The producer interface of a channel.
     *
     * @param name Simple name of the interface.
     * @param packageName Package of the interface.
     * @param description Documentation lines of the interface.
     * @param topicAddressConstantName Name of the topic address constant.
     * @param topicAddress Topic address as a string literal body.
     * @param sendMethods Send methods, one per message and payload type.
     * @param anyType Name of the language's top type, used where an example has no key type.
     * @param clientContractAnnotation Simple name of the annotation on the interface, if configured.
     * @param imports Imports of the source file.
     */
    data class Producer(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val topicAddressConstantName: String,
        val topicAddress: String,
        val sendMethods: List<SendMethod>,
        val anyType: String,
        val clientContractAnnotation: String? = null,
        val imports: List<String> = emptyList(),
    ) : SpringKafkaClientView {
        val hasSingleMethod: Boolean get() = sendMethods.size == 1
        val hasMultipleMessageTypes: Boolean get() = sendMethods.map { it.messageName }.distinct().size > 1
        val hasAdditionalPayloadMethods: Boolean get() =
            sendMethods.any { method -> method.additionalPayloadType != null }
        val hasSinglePayloadMethod: Boolean get() = hasSingleMethod && sendMethods.single().hasPayload
        val hasSingleContractPayloadMethod: Boolean get() =
            hasSinglePayloadMethod && sendMethods.single().additionalPayloadType == null
        val additionalPayloadExampleMethods: List<SendMethod> get() =
            sendMethods
                .filter { method -> method.additionalPayloadType != null }
                .takeIf { methods ->
                    sendMethods.map { method -> method.messageName }.distinct().size == 1 &&
                        methods.size == 1
                }.orEmpty()
        val hasAdditionalPayloadExample: Boolean get() = additionalPayloadExampleMethods.isNotEmpty()
        val hasAdditionalPayloadExampleHeaders: Boolean get() =
            additionalPayloadExampleMethods.singleOrNull()?.hasHeaders == true
        val implementationExamplePayloadType: String? get() =
            additionalPayloadExampleMethods.singleOrNull()?.payloadType
        val implementationExampleKeyType: String get() =
            additionalPayloadExampleMethods.singleOrNull()?.keyParameter?.typeName ?: anyType
    }

    /**
     * A listener method.
     *
     * @param messageName Source-code name of the message.
     * @param methodName Name of the method.
     * @param payloadType Payload parameter type, or `null` when the message has no payload.
     * @param payloadDescription Documentation lines of the payload parameter.
     * @param keyParameter Record key parameter, if the message declares a key.
     * @param headerProperties Header parameters.
     * @param payloadParameterAnnotation Simple name of the annotation on the payload parameter, if configured.
     * @param requiredHeaderAnnotation Simple name of the annotation on required fixed header parameters, if any.
     */
    data class ConsumerMethod(
        val messageName: String,
        val methodName: String,
        val payloadType: String?,
        val payloadDescription: List<String> = emptyList(),
        val keyParameter: KeyParameter? = null,
        val headerProperties: List<HeaderProperty> = emptyList(),
        val payloadParameterAnnotation: String? = null,
        val requiredHeaderAnnotation: String? = null,
    ) {
        val hasPayload: Boolean get() = payloadType != null
        val hasHeaders: Boolean get() = headerProperties.isNotEmpty()
        val hasAdditionalParameters: Boolean get() = keyParameter != null || hasHeaders
        val payloadDescriptionFirstLine: String? get() = payloadDescription.firstOrNull()
        val payloadDescriptionTailLines: List<String> get() = payloadDescription.drop(1)
    }

    /**
     * A send method.
     *
     * @param messageName Source-code name of the message.
     * @param methodName Name of the method.
     * @param payloadType Payload parameter type, or `null` when the message has no payload.
     * @param payloadDescription Documentation lines of the payload parameter.
     * @param payloadBindingAnnotation Annotation binding the payload parameter, if the message has a payload.
     * @param keyParameter Record key parameter, if the message declares a key.
     * @param headerProperties Header parameters.
     * @param payloadParameterAnnotation Simple name of the annotation on the payload parameter, if configured.
     * @param additionalPayloadType Serialized payload type the method sends, or `null` for the contract payload.
     */
    data class SendMethod(
        val messageName: String,
        val methodName: String,
        val payloadType: String?,
        val payloadDescription: List<String> = emptyList(),
        val payloadBindingAnnotation: String? = null,
        val keyParameter: KeyParameter? = null,
        val headerProperties: List<HeaderProperty> = emptyList(),
        val payloadParameterAnnotation: String? = null,
        val additionalPayloadType: AdditionalProducerPayloadType? = null,
    ) {
        val hasPayload: Boolean get() = payloadType != null
        val hasHeaders: Boolean get() = headerProperties.isNotEmpty()
        val hasAdditionalParameters: Boolean get() = keyParameter != null || hasHeaders
        val hasParameterDocumentation: Boolean get() =
            payloadDescription.isNotEmpty() ||
                keyParameter?.description?.isNotEmpty() == true ||
                headerProperties.any { it.description.isNotEmpty() }
        val payloadDescriptionFirstLine: String? get() = payloadDescription.firstOrNull()
        val payloadDescriptionTailLines: List<String> get() = payloadDescription.drop(1)
    }

    /**
     * The record key parameter of a method.
     *
     * @param parameterName Name of the parameter.
     * @param typeName Parameter type.
     * @param description Documentation lines of the parameter.
     * @param required Whether the key must be present.
     * @param annotations Annotations on the parameter.
     * @param parameterSuffix Text after the parameter: a comma when more parameters follow.
     */
    data class KeyParameter(
        val parameterName: String,
        val typeName: String,
        val description: List<String> = emptyList(),
        val required: Boolean,
        val annotations: List<String> = emptyList(),
        val parameterSuffix: String = "",
    ) {
        val descriptionFirstLine: String? get() = description.firstOrNull()
        val descriptionTailLines: List<String> get() = description.drop(1)
    }

    /**
     * A header parameter of a method.
     *
     * @param wireName Name of the header on the record.
     * @param wireNameLiteral Wire name escaped for a string literal.
     * @param parameterName Name of the parameter.
     * @param typeName Parameter type.
     * @param description Documentation lines of the parameter.
     * @param required Whether the contract requires the header.
     * @param defaultValue Default argument of an optional header, where the language has default arguments.
     * @param requiredAnnotation Annotation marking the parameter as not null, where the language uses one.
     * @param nullableAnnotation Annotation marking the parameter as nullable, where the language uses one.
     * @param parameterSuffix Text after the parameter: a comma when more parameters follow.
     * @param bindingAnnotation Annotation binding the parameter to its header, on send methods.
     */
    data class HeaderProperty(
        val wireName: String,
        val wireNameLiteral: String,
        val parameterName: String,
        val typeName: String,
        val description: List<String> = emptyList(),
        val required: Boolean = false,
        val defaultValue: String? = null,
        val requiredAnnotation: String? = null,
        val nullableAnnotation: String? = null,
        val parameterSuffix: String = "",
        val bindingAnnotation: String? = null,
    ) {
        val descriptionFirstLine: String? get() = description.firstOrNull()
        val descriptionTailLines: List<String> get() = description.drop(1)
    }
}
