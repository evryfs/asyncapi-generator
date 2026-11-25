package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.kafka.spring.client.SpringKafkaClientEmitter
import dev.banking.asyncapi.generator.core.generator.kafka.spring.client.SpringKafkaClientSyntax
import dev.banking.asyncapi.generator.core.generator.kafka.spring.client.SpringKafkaClientViewFactory

/** A Spring Kafka client emitter for [syntax] with the given client configuration. */
internal fun springKafkaClientEmitter(
    syntax: SpringKafkaClientSyntax,
    clientPackage: String,
    modelPackage: String,
    generateProducers: Boolean = true,
    additionalPayloadTypes: Set<AdditionalProducerPayloadType> = emptySet(),
    generateConsumers: Boolean = true,
    topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
): SpringKafkaClientEmitter =
    SpringKafkaClientEmitter(
        syntax = syntax,
        viewFactory =
            SpringKafkaClientViewFactory(
                syntax = syntax,
                clientPackage = clientPackage,
                modelPackage = modelPackage,
                generateProducers = generateProducers,
                additionalPayloadTypes = additionalPayloadTypes,
                generateConsumers = generateConsumers,
                topicParameterProperties = topicParameterProperties,
                validationAnnotations = validationAnnotations,
            ),
    )
