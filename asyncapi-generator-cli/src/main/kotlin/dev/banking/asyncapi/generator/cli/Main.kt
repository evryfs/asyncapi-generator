package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationFactory
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import java.io.File

fun main(args: Array<String>) = AsyncApiGeneratorCli().main(args)

class AsyncApiGeneratorCli : CliktCommand(name = "asyncapi-generator") {
    private val input by option("--input", "-i", help = "Path to AsyncAPI YAML file")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .required()
    private val codegenOutputDirectory by option("--codegen-output", help = "Codegen output directory")
        .file(canBeFile = false)
        .default(File("./generated-sources/asyncapi"))
    private val resourceOutputDirectory by option("--resource-output", help = "Resource output directory")
        .file(canBeFile = false)
        .default(File("./generated-resources/asyncapi"))
    private val outputFile by option("--output-file", help = "Write bundled AsyncAPI YAML to file")
        .file(canBeDir = false)

    private val generator by option("--generator", "-g", help = "Target language (default: kotlin)")
        .choice(
            GeneratorName.KOTLIN.configurationValue to GeneratorName.KOTLIN,
            GeneratorName.JAVA.configurationValue to GeneratorName.JAVA,
        ).default(GeneratorName.KOTLIN)

    private val modelsPackage by option("--models-package", help = "Package for generated models")

    private val modelsAnnotation by option(
        "--models-annotation",
        help = "Fully qualified annotation added to generated model classes",
    )

    private val modelsJavaModelType by option(
        "--models-java-model-type",
        help = "Java model shape for generated models (default: class)",
    ).choice(
        JavaModelType.CLASS.configurationValue to JavaModelType.CLASS,
        JavaModelType.RECORD.configurationValue to JavaModelType.RECORD,
    )

    private val schemasAvroProjection by option(
        "--schemas-avro-projection",
        help = "Enable Avro projection schema generation",
    ).flag(default = false)

    private val schemasAvroProjectionPackage by option(
        "--schemas-avro-projection-package",
        help = "Package for generated Avro projection schemas",
    )

    private val schemasNativeAvro by option(
        "--schemas-native-avro",
        help = "Enable native Avro schema artifact generation",
    ).flag(default = false)

    private val schemasNativeAvroGenerateSpecificRecords by option(
        "--schemas-native-avro-generate-specific-records",
        help = "Generate Apache Avro Java SpecificRecord classes for native Avro schemas (default: true)",
    ).choice(
        "true" to true,
        "false" to false,
    )

    private val schemasNativeProtobuf by option(
        "--schemas-native-protobuf",
        help = "Enable native Protobuf schema artifact generation",
    ).flag(default = false)

    private val schemasNativeProtobufGenerateJavaMessageTypes by option(
        "--schemas-native-protobuf-generate-java-message-types",
        help = "Generate Java Protobuf message classes for native Protobuf schemas (default: true)",
    ).choice(
        "true" to true,
        "false" to false,
    )

    private val clientsSpringKafka by option(
        "--clients-spring-kafka",
        help = "Enable Spring Kafka client generation",
    ).flag(default = false)

    private val clientsSpringKafkaPackage by option(
        "--clients-spring-kafka-package",
        help = "Package for generated Spring Kafka clients",
    )

    private val clientsSpringKafkaModelPackage by option(
        "--clients-spring-kafka-model-package",
        help = "Package containing model types used by generated Spring Kafka clients",
    )

    private val clientsQuarkusKafka by option(
        "--clients-quarkus-kafka",
        help = "Enable Quarkus Kafka client generation",
    ).flag(default = false)

    private val clientsQuarkusKafkaPackage by option(
        "--clients-quarkus-kafka-package",
        help = "Package for generated Quarkus Kafka clients",
    )

    private val clientsQuarkusKafkaModelPackage by option(
        "--clients-quarkus-kafka-model-package",
        help = "Package containing model types used by generated Quarkus Kafka clients",
    )

    override fun run() {
        echo("Generating AsyncAPI code from $input...")

        val context = AsyncApiContext()
        val root = AsyncApiRegistry.readYaml(input, context)
        val parser = AsyncApiParser(context)
        val document = parser.parse(root)

        val validator = AsyncApiValidator(context)
        val results = validator.validate(document)

        if (results.hasWarnings()) {
            results.warnings.forEach { echo("WARN: ${it.message}") }
        }

        if (results.hasErrors()) {
            results.errors.forEach { echo("ERROR: ${it.message}") }
            throw RuntimeException("Validation failed with ${results.errors.size} errors.")
        }

        val bundler = AsyncApiBundler()
        val bundledDoc = bundler.bundle(document)
        outputFile?.let { file ->
            AsyncApiRegistry.writeYaml(file, bundledDoc)
        }
        val sourceRootName =
            if (generator == GeneratorName.KOTLIN) {
                "src/main/kotlin"
            } else {
                "src/main/java"
        }
        val sourceRoot = codegenOutputDirectory.resolve(sourceRootName)
        val javaSourceRoot = codegenOutputDirectory.resolve("src/main/java")
        val generatorConfiguration =
            try {
                GeneratorConfigurationFactory.create(
                    GeneratorConfigurationRequest(
                        language = generator,
                        sourceOutputDirectory = sourceRoot,
                        javaSourceOutputDirectory = javaSourceRoot,
                        resourceOutputDirectory = resourceOutputDirectory,
                        models = modelRequest(),
                        schemas = schemaRequest(),
                        clients = clientRequest(),
                    ),
                )
            } catch (exception: IllegalArgumentException) {
                throw UsageError(exception.message ?: "Invalid generator configuration")
            }
        if (generatorConfiguration.hasConfiguredOutputs()) {
            AsyncApiGenerator().generate(bundledDoc, generatorConfiguration)
        }
        echo("Generation complete.")
    }

    private fun modelRequest(): GeneratorConfigurationRequest.Models? =
        GeneratorConfigurationRequest.models(
            packageName = modelsPackage,
            annotation = modelsAnnotation,
            javaModelType = modelsJavaModelType?.configurationValue,
        )

    private fun schemaRequest(): GeneratorConfigurationRequest.Schemas =
        GeneratorConfigurationRequest.Schemas(
            avroProjection =
                GeneratorConfigurationRequest.avroProjection(
                    enabled = true.takeIf { schemasAvroProjection },
                    packageName = schemasAvroProjectionPackage,
                ),
            nativeAvro =
                GeneratorConfigurationRequest.nativeAvro(
                    enabled = true.takeIf { schemasNativeAvro },
                    generateSpecificRecords = schemasNativeAvroGenerateSpecificRecords,
                ),
            nativeProtobuf =
                GeneratorConfigurationRequest.nativeProtobuf(
                    enabled = true.takeIf { schemasNativeProtobuf },
                    generateJavaMessageTypes = schemasNativeProtobufGenerateJavaMessageTypes,
                ),
        )

    private fun clientRequest(): GeneratorConfigurationRequest.Clients =
        GeneratorConfigurationRequest.Clients(
            springKafka =
                GeneratorConfigurationRequest.springKafka(
                    enabled = true.takeIf { clientsSpringKafka },
                    packageName = clientsSpringKafkaPackage,
                    modelPackageName = clientsSpringKafkaModelPackage,
                ),
            quarkusKafka =
                GeneratorConfigurationRequest.quarkusKafka(
                    enabled = true.takeIf { clientsQuarkusKafka },
                    packageName = clientsQuarkusKafkaPackage,
                    modelPackageName = clientsQuarkusKafkaModelPackage,
                ),
        )
}
