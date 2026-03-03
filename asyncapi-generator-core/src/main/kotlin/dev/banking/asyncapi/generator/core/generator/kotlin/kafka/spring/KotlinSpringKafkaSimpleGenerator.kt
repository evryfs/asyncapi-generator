package dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.kotlin.factory.KotlinSpringKafkaSimpleModelFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import java.io.File

class KotlinSpringKafkaSimpleGenerator(
    outputDir: File,
    clientPackage: String,
    modelPackage: String,
) {
    private val modelFactory = KotlinSpringKafkaSimpleModelFactory(clientPackage, modelPackage)
    private val producerGenerator = KotlinSpringKafkaSimpleProducerGenerator(outputDir)
    private val consumerGenerator = KotlinSpringKafkaSimpleConsumerGenerator(outputDir)

    fun generate(channels: List<AnalyzedChannel>) {
        channels.forEach { channel ->
            val items = modelFactory.create(channel)
            items.forEach { item ->
                when (item) {
                    is GeneratorItem.KafkaProducerClass -> producerGenerator.generate(item)
                    is GeneratorItem.KafkaHandlerInterface -> consumerGenerator.generate(item)
                    else -> { /* Ignore */ }
                }
            }
        }
    }
}
