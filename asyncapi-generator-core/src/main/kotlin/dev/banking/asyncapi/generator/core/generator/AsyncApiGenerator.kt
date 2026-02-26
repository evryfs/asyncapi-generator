package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.generator.analyzer.ChannelAnalyzer
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinGenerator
import dev.banking.asyncapi.generator.core.generator.analyzer.SchemaAnalyzer
import dev.banking.asyncapi.generator.core.generator.avro.AvroGenerator
import dev.banking.asyncapi.generator.core.generator.model.GeneratorOptions
import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.JAVA
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.KOTLIN
import dev.banking.asyncapi.generator.core.generator.java.JavaGenerator
import dev.banking.asyncapi.generator.core.generator.java.factory.JavaGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.java.kafka.spring.JavaSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.generator.kotlin.factory.KotlinGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring.KotlinSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.generator.loader.AsyncApiSchemaLoader
import dev.banking.asyncapi.generator.core.generator.normalizer.SchemaNormalizer
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import org.slf4j.LoggerFactory

class AsyncApiGenerator {

    private val log = LoggerFactory.getLogger(AsyncApiGenerator::class.java)

    private val schemaAnalyzer = SchemaAnalyzer()
    private val schemaNormalizer = SchemaNormalizer()

    fun generate(
        asyncApiDocument: AsyncApiDocument,
        generatorOptions: GeneratorOptions,
    ) {
        val schemas = AsyncApiSchemaLoader.load(asyncApiDocument)
        val normalizedSchemas = schemaNormalizer.normalize(schemas)
        val (analyzedSchemas, polymorphic) = schemaAnalyzer.analyze(normalizedSchemas)

        val context = GeneratorContext(analyzedSchemas)

        val channelAnalyzer = ChannelAnalyzer()
        val analyzedChannels = channelAnalyzer.analyze(asyncApiDocument).channels

        when (generatorOptions.generatorName) {
            KOTLIN -> {
                if (generatorOptions.generateModels) {
                    val annotation = generatorOptions.configOptions["model.annotation"]
                    val factory = KotlinGeneratorModelFactory(
                        generatorOptions.modelPackage,
                        context,
                        polymorphic,
                        annotation
                    )
                    val kotlinGenerationModel = analyzedSchemas.mapNotNull { (name, schema) ->
                        factory.create(name, schema)
                    }
                    val kotlinModelGenerator = KotlinGenerator(
                        packageName = generatorOptions.modelPackage,
                        outputDir = generatorOptions.codegenOutputDirectory,
                        generationModel = kotlinGenerationModel,
                    )
                    kotlinModelGenerator.generate()
                }

                if (generatorOptions.generateSpringKafkaClient) {
                    val kafkaGenerator = KotlinSpringKafkaGenerator(
                        outputDir = generatorOptions.codegenOutputDirectory,
                        clientPackage = generatorOptions.clientPackage,
                        modelPackage = generatorOptions.modelPackage,
                    )
                    kafkaGenerator.generate(analyzedChannels)
                }

                if (generatorOptions.generateQuarkusKafkaClient) {
                    log.info("Generate Kotlin Quarkus Kafka Client is not yet implemented. Skipping..")
                }
            }

            JAVA -> {
                if (generatorOptions.generateModels) {
                    val factory = JavaGeneratorModelFactory(generatorOptions.modelPackage, context, polymorphic)
                    val javaGenerationModel = analyzedSchemas.mapNotNull { (name, schema) ->
                        factory.create(name, schema)
                    }
                    val javaGenerator = JavaGenerator(
                        packageName = generatorOptions.modelPackage,
                        outputDir = generatorOptions.codegenOutputDirectory,
                        generationModel = javaGenerationModel,
                    )
                    javaGenerator.generate()
                }
                if (generatorOptions.generateSpringKafkaClient) {
                    val kafkaGenerator = JavaSpringKafkaGenerator(
                        outputDir = generatorOptions.codegenOutputDirectory,
                        clientPackage = generatorOptions.clientPackage,
                        modelPackage = generatorOptions.modelPackage,
                    )
                    kafkaGenerator.generate(analyzedChannels)
                }

                if (generatorOptions.generateQuarkusKafkaClient) {
                    log.info("Generate Java Quarkus Kafka Client is not yet implemented. Skipping..")
                }
            }
        }

        if (generatorOptions.generateAvroSchema) {
            val avroGenerator = AvroGenerator(
                outputDir = generatorOptions.resourceOutputDirectory,
                packageName = generatorOptions.schemaPackage,
            )
            avroGenerator.generate(analyzedSchemas)
        }
    }
}
