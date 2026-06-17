package dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.kotlin.factory.KotlinSpringKafkaModelFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import java.io.File

class KotlinSpringKafkaGenerator(
    outputDir: File,
    clientPackage: String,
    modelPackage: String,
    generateProducers: Boolean = true,
    generateConsumers: Boolean = true,
) {
    private val modelFactory =
        KotlinSpringKafkaModelFactory(
            clientPackage = clientPackage,
            modelPackage = modelPackage,
            generateProducers = generateProducers,
            generateConsumers = generateConsumers,
        )
    private val producerGenerator = KotlinSpringKafkaProducerGenerator(outputDir)
    private val consumerGenerator = KotlinSpringKafkaConsumerGenerator(outputDir)

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
