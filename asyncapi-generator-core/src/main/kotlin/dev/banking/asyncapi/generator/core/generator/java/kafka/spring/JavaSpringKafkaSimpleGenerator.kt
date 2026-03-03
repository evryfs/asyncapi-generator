package dev.banking.asyncapi.generator.core.generator.java.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.java.factory.JavaSpringKafkaSimpleModelFactory
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import java.io.File

class JavaSpringKafkaSimpleGenerator(
    outputDir: File,
    clientPackage: String,
    modelPackage: String,
) {
    private val modelFactory = JavaSpringKafkaSimpleModelFactory(clientPackage, modelPackage)
    private val producerGenerator = JavaSpringKafkaSimpleProducerGenerator(outputDir)
    private val consumerGenerator = JavaSpringKafkaSimpleConsumerGenerator(outputDir)

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
