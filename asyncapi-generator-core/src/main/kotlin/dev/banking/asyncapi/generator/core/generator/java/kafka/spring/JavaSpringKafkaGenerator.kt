package dev.banking.asyncapi.generator.core.generator.java.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.java.factory.JavaKafkaGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.kafka.spring.AutoConfigurationModel
import dev.banking.asyncapi.generator.core.generator.kafka.spring.AutoConfigurationResourceGenerator
import java.io.File

class JavaSpringKafkaGenerator(
    outputDir: File,
    private val clientPackage: String,
    modelPackage: String,
    topicPropertyPrefix: String,
    topicPropertySuffix: String,
    resourceOutputDir: File,
) {
    private val modelFactory =
        JavaKafkaGeneratorModelFactory(
            this.clientPackage,
            modelPackage,
            topicPropertyPrefix,
            topicPropertySuffix,
        )

    private val handlerGenerator = JavaSpringKafkaHandlerGenerator(outputDir)
    private val listenerGenerator = JavaSpringKafkaListenerGenerator(outputDir)
    private val producerGenerator = JavaSpringKafkaProducerGenerator(outputDir)
    private val autoConfigGenerator = JavaSpringKafkaAutoConfigurationGenerator(outputDir)
    private val autoConfigResourceGenerator = AutoConfigurationResourceGenerator(resourceOutputDir)

    fun generate(channels: List<AnalyzedChannel>) {
        val autoConfigPackage = "${this.clientPackage}.config"
        val autoConfigClass = "AsyncApiKafkaAutoConfiguration"
        autoConfigGenerator.generate(
            AutoConfigurationModel(
                packageName = autoConfigPackage,
                className = autoConfigClass,
                clientPackage = this.clientPackage,
            ),
        )
        autoConfigResourceGenerator.generate("$autoConfigPackage.$autoConfigClass")

        channels.forEach { channel ->
            val items = modelFactory.create(channel)
            items.forEach { item ->
                when (item) {
                    is GeneratorItem.KafkaHandlerInterface -> handlerGenerator.generate(item)
                    is GeneratorItem.KafkaListenerClass -> listenerGenerator.generate(item)
                    is GeneratorItem.KafkaProducerClass -> producerGenerator.generate(item)
                    else -> { /* Ignore */
                    }
                }
            }
        }
    }
}
