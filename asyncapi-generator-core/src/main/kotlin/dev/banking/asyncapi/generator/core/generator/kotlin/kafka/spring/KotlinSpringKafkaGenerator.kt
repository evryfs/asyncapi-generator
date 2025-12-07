package dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.kotlin.factory.KotlinKafkaGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import java.io.File

class KotlinSpringKafkaGenerator(
    outputDir: File,
    clientPackage: String,
    modelPackage: String,
) {

    private val modelFactory = KotlinKafkaGeneratorModelFactory(clientPackage, modelPackage)
    private val handlerGenerator = KotlinSpringKafkaHandlerGenerator(outputDir)
    private val listenerGenerator = KotlinSpringKafkaListenerGenerator(outputDir)
    private val producerGenerator = KotlinSpringKafkaProducerGenerator(outputDir)
    private val messageGenerator = KotlinSpringKafkaMessageGenerator(outputDir, clientPackage)

    fun generate(channels: List<AnalyzedChannel>) {
        if (channels.isNotEmpty()) {
            messageGenerator.generate()
        }
        channels.forEach { channel ->
            val items = modelFactory.create(channel)
            items.forEach { item ->
                when (item) {
                    is GeneratorItem.KafkaHandlerInterface -> handlerGenerator.generate(item)
                    is GeneratorItem.KafkaListenerClass -> listenerGenerator.generate(item)
                    is GeneratorItem.KafkaProducerClass -> producerGenerator.generate(item)
                    else -> { /* Ignore other types if mixed */ }
                }
            }
        }
    }
}
