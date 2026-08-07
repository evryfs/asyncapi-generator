package dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.kotlin.factory.KotlinSpringKafkaModelFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult

class KotlinSpringKafkaGenerator(
    clientPackage: String,
    modelPackage: String,
    generateProducers: Boolean = true,
    additionalPayloadTypes: Set<AdditionalProducerPayloadType> = emptySet(),
    generateConsumers: Boolean = true,
    topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
) {
    private val modelFactory =
        KotlinSpringKafkaModelFactory(
            clientPackage = clientPackage,
            modelPackage = modelPackage,
            generateProducers = generateProducers,
            additionalPayloadTypes = additionalPayloadTypes,
            generateConsumers = generateConsumers,
            topicParameterProperties = topicParameterProperties,
            validationAnnotations = validationAnnotations,
        )
    private val producerGenerator = KotlinSpringKafkaProducerGenerator()
    private val consumerGenerator = KotlinSpringKafkaConsumerGenerator()

    fun render(channels: List<AnalyzedChannel>): GenerationResult =
        GenerationResult(
            channels.flatMap { channel ->
                modelFactory.create(channel).map { item ->
                    when (item) {
                        is GeneratorItem.KafkaProducerClass -> producerGenerator.render(item)
                        is GeneratorItem.KafkaConsumerInterface -> consumerGenerator.render(item)
                        else -> error("Unexpected Kotlin Spring Kafka model: ${item::class.simpleName}")
                    }
                }
            },
        )
}
