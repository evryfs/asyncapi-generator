package dev.banking.asyncapi.generator.core.generator.java.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.java.factory.JavaSpringKafkaModelFactory
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult

class JavaSpringKafkaGenerator(
    clientPackage: String,
    modelPackage: String,
    generateProducers: Boolean = true,
    generateConsumers: Boolean = true,
    topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
) {
    private val modelFactory =
        JavaSpringKafkaModelFactory(
            clientPackage = clientPackage,
            modelPackage = modelPackage,
            generateProducers = generateProducers,
            generateConsumers = generateConsumers,
            topicParameterProperties = topicParameterProperties,
            validationAnnotations = validationAnnotations,
        )
    private val producerGenerator = JavaSpringKafkaProducerGenerator()
    private val consumerGenerator = JavaSpringKafkaConsumerGenerator()

    fun render(channels: List<AnalyzedChannel>): GenerationResult =
        GenerationResult(
            channels.flatMap { channel ->
                modelFactory.create(channel).mapNotNull { item ->
                    when (item) {
                        is GeneratorItem.KafkaProducerClass -> producerGenerator.render(item)
                        is GeneratorItem.KafkaConsumerInterface -> consumerGenerator.render(item)
                        else -> null
                    }
                }
            },
        )
}
