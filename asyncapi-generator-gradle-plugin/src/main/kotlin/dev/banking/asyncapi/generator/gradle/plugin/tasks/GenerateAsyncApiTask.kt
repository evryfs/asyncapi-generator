package dev.banking.asyncapi.generator.gradle.plugin.tasks

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationFactory
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.JAVA
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.KOTLIN
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Codegen output is cheap to reproduce and not worth caching")
abstract class GenerateAsyncApiTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val codegenOutputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val resourceOutputDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val modelsEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val modelsPackageName: Property<String>

    @get:Input
    @get:Optional
    abstract val modelsAnnotation: Property<String>

    @get:Input
    @get:Optional
    abstract val modelsJavaModelType: Property<String>

    @get:Input
    @get:Optional
    abstract val avroProjectionEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val avroProjectionPackageName: Property<String>

    @get:Input
    @get:Optional
    abstract val nativeAvroEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val nativeAvroGenerateSpecificRecords: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val nativeProtobufEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val nativeProtobufGenerateJavaMessageTypes: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val kafkaEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val kafkaPackageName: Property<String>

    @get:Input
    @get:Optional
    abstract val kafkaModelPackageName: Property<String>

    @get:Input
    @get:Optional
    abstract val kafkaHeadersEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val kafkaSpringKafkaEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val kafkaSpringKafkaProducerEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val kafkaSpringKafkaConsumerEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val quarkusKafkaEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val quarkusKafkaPackageName: Property<String>

    @get:Input
    @get:Optional
    abstract val quarkusKafkaModelPackageName: Property<String>

    @get:Input
    abstract val generatorName: Property<String>

    @TaskAction
    fun generate() {
        logger.lifecycle("asyncapi-generator-gradle-plugin started")

        val context = AsyncApiContext()
        val parser = AsyncApiParser(context)
        val validator = AsyncApiValidator(context)
        val bundler = AsyncApiBundler()
        val generator = AsyncApiGenerator()

        val root = AsyncApiRegistry.readYaml(inputFile.get().asFile, context)
        val asyncApiDocument = parser.parse(root)
        val validationErrors = validator.validate(asyncApiDocument)

        validationErrors.logWarnings()
        validationErrors.throwErrors()

        val bundled = bundler.bundle(asyncApiDocument)

        if (outputFile.isPresent) {
            val file = outputFile.get().asFile
            logger.lifecycle("Writing bundled AsyncAPI specification to: ${file.absolutePath}")
            AsyncApiRegistry.writeYaml(file, bundled)
        }

        val genNameString = generatorName.get()
        val targetLanguage =
            GeneratorName.fromConfigurationValue(
                value = genNameString,
                path = "generatorName",
            )

        // Calculate Source Root
        val sourceRootName =
            when (targetLanguage) {
                KOTLIN -> "src/main/kotlin"
                JAVA -> "src/main/java"
        }
        val codegenSourceRoot = codegenOutputDirectory.get().asFile.resolve(sourceRootName)
        val javaSourceRoot = codegenOutputDirectory.get().asFile.resolve("src/main/java")

        val generatorConfiguration =
            GeneratorConfigurationFactory.create(
                GeneratorConfigurationRequest(
                    language = targetLanguage,
                    sourceOutputDirectory = codegenSourceRoot,
                    javaSourceOutputDirectory = javaSourceRoot,
                    resourceOutputDirectory = resourceOutputDirectory.get().asFile,
                    models =
                        modelRequest(
                            enabled = modelsEnabled.orNull,
                            packageName = modelsPackageName.orNull,
                            annotation = modelsAnnotation.orNull,
                            javaModelType = modelsJavaModelType.orNull,
                        ),
                    schemas =
                        schemaRequest(
                            avroProjectionEnabled = avroProjectionEnabled.orNull,
                            avroProjectionPackageName = avroProjectionPackageName.orNull,
                            nativeAvroEnabled = nativeAvroEnabled.orNull,
                            nativeAvroGenerateSpecificRecords = nativeAvroGenerateSpecificRecords.orNull,
                            nativeProtobufEnabled = nativeProtobufEnabled.orNull,
                            nativeProtobufGenerateJavaMessageTypes = nativeProtobufGenerateJavaMessageTypes.orNull,
                        ),
                    clients =
                        clientRequest(
                            kafkaEnabled = kafkaEnabled.orNull,
                            kafkaPackageName = kafkaPackageName.orNull,
                            kafkaModelPackageName = kafkaModelPackageName.orNull,
                            kafkaHeadersEnabled = kafkaHeadersEnabled.orNull,
                            kafkaSpringKafkaEnabled = kafkaSpringKafkaEnabled.orNull,
                            kafkaSpringKafkaProducerEnabled = kafkaSpringKafkaProducerEnabled.orNull,
                            kafkaSpringKafkaConsumerEnabled = kafkaSpringKafkaConsumerEnabled.orNull,
                            quarkusKafkaEnabled = quarkusKafkaEnabled.orNull,
                            quarkusKafkaPackageName = quarkusKafkaPackageName.orNull,
                            quarkusKafkaModelPackageName = quarkusKafkaModelPackageName.orNull,
                        ),
                ),
            )
        if (generatorConfiguration.hasConfiguredOutputs()) {
            generator.generate(bundled, generatorConfiguration)
        }
        logger.lifecycle("asyncapi-generator-gradle-plugin completed")
    }

    private fun modelRequest(
        enabled: Boolean?,
        packageName: String?,
        annotation: String?,
        javaModelType: String?,
    ): GeneratorConfigurationRequest.Models? =
        GeneratorConfigurationRequest.models(
            enabled = enabled,
            packageName = packageName,
            annotation = annotation,
            javaModelType = javaModelType,
        )

    private fun schemaRequest(
        avroProjectionEnabled: Boolean?,
        avroProjectionPackageName: String?,
        nativeAvroEnabled: Boolean?,
        nativeAvroGenerateSpecificRecords: Boolean?,
        nativeProtobufEnabled: Boolean?,
        nativeProtobufGenerateJavaMessageTypes: Boolean?,
    ): GeneratorConfigurationRequest.Schemas =
        GeneratorConfigurationRequest.Schemas(
            avroProjection =
                GeneratorConfigurationRequest.avroProjection(
                    enabled = avroProjectionEnabled,
                    packageName = avroProjectionPackageName,
                ),
            nativeAvro =
                GeneratorConfigurationRequest.nativeAvro(
                    enabled = nativeAvroEnabled,
                    generateSpecificRecords = nativeAvroGenerateSpecificRecords,
                ),
            nativeProtobuf =
                GeneratorConfigurationRequest.nativeProtobuf(
                    enabled = nativeProtobufEnabled,
                    generateJavaMessageTypes = nativeProtobufGenerateJavaMessageTypes,
                ),
        )

    private fun clientRequest(
        kafkaEnabled: Boolean?,
        kafkaPackageName: String?,
        kafkaModelPackageName: String?,
        kafkaHeadersEnabled: Boolean?,
        kafkaSpringKafkaEnabled: Boolean?,
        kafkaSpringKafkaProducerEnabled: Boolean?,
        kafkaSpringKafkaConsumerEnabled: Boolean?,
        quarkusKafkaEnabled: Boolean?,
        quarkusKafkaPackageName: String?,
        quarkusKafkaModelPackageName: String?,
    ): GeneratorConfigurationRequest.Clients =
        GeneratorConfigurationRequest.Clients(
            kafka =
                kafkaRequest(
                    enabled = kafkaEnabled,
                    packageName = kafkaPackageName,
                    modelPackageName = kafkaModelPackageName,
                    headersEnabled = kafkaHeadersEnabled,
                    springKafkaEnabled = kafkaSpringKafkaEnabled,
                    springKafkaProducerEnabled = kafkaSpringKafkaProducerEnabled,
                    springKafkaConsumerEnabled = kafkaSpringKafkaConsumerEnabled,
                ),
            quarkusKafka =
                GeneratorConfigurationRequest.quarkusKafka(
                    enabled = quarkusKafkaEnabled,
                    packageName = quarkusKafkaPackageName,
                    modelPackageName = quarkusKafkaModelPackageName,
                ),
        )

    private fun kafkaRequest(
        enabled: Boolean?,
        packageName: String?,
        modelPackageName: String?,
        headersEnabled: Boolean?,
        springKafkaEnabled: Boolean?,
        springKafkaProducerEnabled: Boolean?,
        springKafkaConsumerEnabled: Boolean?,
    ): GeneratorConfigurationRequest.Kafka? =
        GeneratorConfigurationRequest.kafka(
            enabled = enabled,
            packageName = packageName,
            modelPackageName = modelPackageName,
            headers = GeneratorConfigurationRequest.kafkaHeaders(enabled = headersEnabled),
            springKafka =
                GeneratorConfigurationRequest.kafkaSpringKafka(
                    enabled = springKafkaEnabled,
                    producer = GeneratorConfigurationRequest.kafkaProducer(enabled = springKafkaProducerEnabled),
                    consumer = GeneratorConfigurationRequest.kafkaConsumer(enabled = springKafkaConsumerEnabled),
                ),
        )
}
