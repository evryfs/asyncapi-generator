package dev.banking.asyncapi.generator.core.generator.java.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.java.factory.JavaSpringKafkaModelFactory
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import java.io.File

class JavaSpringKafkaGenerator(
    outputDir: File,
    clientPackage: String,
    modelPackage: String,
) {
    private val modelFactory = JavaSpringKafkaModelFactory(clientPackage, modelPackage)
    private val producerGenerator = JavaSpringKafkaProducerGenerator(outputDir)
    private val consumerGenerator = JavaSpringKafkaConsumerGenerator(outputDir)

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
