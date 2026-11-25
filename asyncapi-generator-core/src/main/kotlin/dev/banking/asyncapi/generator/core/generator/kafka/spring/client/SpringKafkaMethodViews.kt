package dev.banking.asyncapi.generator.core.generator.kafka.spring.client

import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaHeaderProperty
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaKeyContract
import dev.banking.asyncapi.generator.core.generator.kafka.spring.KafkaPayload
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaMessageContract
import dev.banking.asyncapi.generator.core.generator.kafka.spring.serializedPayloadDescription
import dev.banking.asyncapi.generator.core.generator.sourcemodel.Constraints

/**
 * Builds the listener and send method views of a message, with their parameters.
 *
 * @param syntax How the source language writes the client.
 * @param validationAnnotations Annotations added to payload and key parameters.
 */
internal class SpringKafkaMethodViews(
    private val syntax: SpringKafkaClientSyntax,
    private val validationAnnotations: ClientValidationAnnotations,
) {
    /** The listener method of [message]. */
    fun consumerMethod(message: SpringKafkaMessageContract): SpringKafkaClientView.ConsumerMethod {
        val payload = message.payload
        val headers = payload.headers(producer = false)
        return SpringKafkaClientView.ConsumerMethod(
            messageName = payload.messageName,
            methodName = message.consumerMethodName,
            payloadType = payload.type?.let { type -> syntax.typeName(type) },
            payloadDescription = payload.contractPayloadDescription(),
            keyParameter =
                message.keyContract?.keyParameter(
                    parameterName = "receivedKey",
                    consumer = true,
                    hasFollowingParameters = headers.isNotEmpty(),
                ),
            headerProperties = headers,
            payloadParameterAnnotation =
                validationAnnotations.payloadParameter?.simpleName?.takeIf { payload.hasPayload },
            requiredHeaderAnnotation = syntax.nullabilityAnnotation(nullable = false)?.removePrefix("@"),
        )
    }

    /** The send methods of [message], one for the contract payload and one per additional payload type. */
    fun sendMethods(message: SpringKafkaMessageContract): List<SpringKafkaClientView.SendMethod> {
        val payload = message.payload
        val headers = payload.headers(producer = true)
        return message.producerMethods.map { producerMethod ->
            val additionalPayloadType = producerMethod.additionalPayloadType
            SpringKafkaClientView.SendMethod(
                messageName = payload.messageName,
                methodName = producerMethod.methodName,
                payloadType = payload.producerPayloadType(additionalPayloadType),
                payloadDescription = payload.producerPayloadDescription(additionalPayloadType),
                payloadBindingAnnotation = "Payload".takeIf { payload.hasPayload },
                keyParameter =
                    message.keyContract?.keyParameter(
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

    private fun KafkaPayload.producerPayloadType(additionalPayloadType: AdditionalProducerPayloadType?): String? =
        when (additionalPayloadType) {
            AdditionalProducerPayloadType.BYTE_ARRAY -> syntax.byteArrayType
            AdditionalProducerPayloadType.STRING -> "String"
            null -> type?.let { type -> syntax.typeName(type) }
        }

    private fun KafkaPayload.contractPayloadDescription(): List<String> =
        if (hasPayload) {
            syntax.docLines(payloadDescription).ifEmpty { listOf("Message payload.") }
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

    private fun KafkaPayload.headers(producer: Boolean): List<SpringKafkaClientView.HeaderProperty> =
        headerProperties.mapIndexed { index, header ->
            val wireNameLiteral = syntax.stringLiteral(header.wireName)
            SpringKafkaClientView.HeaderProperty(
                wireName = header.wireName,
                wireNameLiteral = wireNameLiteral,
                parameterName = header.parameterName,
                typeName = syntax.typeName(header.type, header.nullable),
                description = header.parameterDescription(),
                required = header.required,
                defaultValue = if (header.required) null else "null",
                requiredAnnotation = syntax.nullabilityAnnotation(nullable = false).takeUnless { header.nullable },
                nullableAnnotation = syntax.nullabilityAnnotation(nullable = true).takeIf { header.nullable },
                parameterSuffix = if (index == headerProperties.lastIndex) "" else ",",
                bindingAnnotation =
                    if (producer) "Header(name = \"$wireNameLiteral\", required = ${header.required})" else null,
            )
        }

    private fun KafkaHeaderProperty.parameterDescription(): List<String> =
        description
            ?.takeIf { value -> value.isNotBlank() }
            ?.let(syntax::docLines)
            ?: listOf("Value of the ${syntax.code(wireName)} Kafka message header.")

    private fun KafkaKeyContract.keyParameter(
        parameterName: String,
        consumer: Boolean,
        hasFollowingParameters: Boolean,
    ): SpringKafkaClientView.KeyParameter {
        val defaultDescription =
            if (consumer && nullable) {
                "Kafka record key, or ${syntax.code("null")} when the record has no key."
            } else {
                "Kafka record key."
            }
        return SpringKafkaClientView.KeyParameter(
            parameterName = parameterName,
            typeName = syntax.typeName(type, nullable),
            description = syntax.docLines(schema.description).ifEmpty { listOf(defaultDescription) },
            required = !nullable,
            annotations =
                syntax.parameterConstraints(Constraints.of(schema)) +
                    listOfNotNull(
                        validationAnnotations.payloadParameter
                            ?.simpleName
                            ?.takeIf { isModel }
                            ?.let { "@$it" },
                        syntax.nullabilityAnnotation(nullable),
                    ),
            parameterSuffix = if (hasFollowingParameters) "," else "",
        )
    }
}
