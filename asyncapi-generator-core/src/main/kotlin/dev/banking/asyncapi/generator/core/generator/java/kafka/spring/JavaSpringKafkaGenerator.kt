package dev.banking.asyncapi.generator.core.generator.java.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.java.factory.JavaKafkaGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import java.io.File

class JavaSpringKafkaGenerator(
    outputDir: File,
    clientPackage: String,
    modelPackage: String
) {
    private val modelFactory = JavaKafkaGeneratorModelFactory(clientPackage, modelPackage)

    private val handlerGenerator = JavaSpringKafkaHandlerGenerator(outputDir)
    private val listenerGenerator = JavaSpringKafkaListenerGenerator(outputDir)
    private val producerGenerator = JavaSpringKafkaProducerGenerator(outputDir)

    fun generate(channels: List<AnalyzedChannel>) {
        channels.forEach { channel ->
            val items = modelFactory.create(channel)
            items.forEach { item ->
                when (item) {
                    is GeneratorItem.KafkaHandlerInterface -> handlerGenerator.generate(item)
                    is GeneratorItem.KafkaListenerClass -> listenerGenerator.generate(item)
                    is GeneratorItem.KafkaProducerClass -> producerGenerator.generate(item)
                    else -> { /* Ignore */ }
                }
            }
        }
    }
}
