package com.tietoevry.banking.asyncapi.generator.core.generator.java.kafka.spring

import com.tietoevry.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import com.tietoevry.banking.asyncapi.generator.core.generator.java.factory.JavaKafkaGeneratorModelFactory
import com.tietoevry.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import java.io.File

class JavaSpringKafkaGenerator(
    outputDir: File,
    clientPackage: String,
    modelPackage: String
) {
    private val modelFactory = JavaKafkaGeneratorModelFactory(clientPackage, modelPackage)

    private val messageGenerator = JavaSpringKafkaMessageGenerator(outputDir, clientPackage)
    private val handlerGenerator = JavaSpringKafkaHandlerGenerator(outputDir)
    private val listenerGenerator = JavaSpringKafkaListenerGenerator(outputDir)
    private val producerGenerator = JavaSpringKafkaProducerGenerator(outputDir)

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
                    else -> { /* Ignore */ }
                }
            }
        }
    }
}
