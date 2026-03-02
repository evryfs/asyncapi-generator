package dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.kafka.spring.AutoConfigurationModel
import dev.banking.asyncapi.generator.core.generator.kafka.spring.AutoConfigurationResourceGenerator
import dev.banking.asyncapi.generator.core.generator.kotlin.factory.KotlinKafkaGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import java.io.File

class KotlinSpringKafkaGenerator(
    outputDir: File,
    private val clientPackage: String,
    modelPackage: String,
    topicPropertyPrefix: String,
    topicPropertySuffix: String,
    resourceOutputDir: File,
) {
    private val modelFactory =
        KotlinKafkaGeneratorModelFactory(
            this.clientPackage,
            modelPackage,
            topicPropertyPrefix,
            topicPropertySuffix,
        )
    private val handlerGenerator = KotlinSpringKafkaHandlerGenerator(outputDir)
    private val listenerGenerator = KotlinSpringKafkaListenerGenerator(outputDir)
    private val producerGenerator = KotlinSpringKafkaProducerGenerator(outputDir)
    private val autoConfigGenerator = KotlinSpringKafkaAutoConfigurationGenerator(outputDir)
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
                    else -> { /* Ignore other types if mixed */ }
                }
            }
        }
    }
}
