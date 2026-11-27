package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.GeneratorSettings
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeneratorConfigurationFactoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `a model package enables source models of the language's default model type`() {
        mapOf(
            "kotlin" to ModelType.KOTLIN_DATA_CLASS,
            "java" to ModelType.JAVA_CLASS,
        ).forEach { (generatorName, modelType) ->
            val configuration = create(settings(generatorName = generatorName, modelPackage = "com.example.model"))

            assertEquals(
                GeneratorName.fromConfigurationValue(generatorName, "generatorName").profile,
                configuration.profile,
            )
            assertEquals(ModelGeneration.Enabled("com.example.model", modelType = modelType), configuration.models)
            assertTrue(configuration.schemas.isEmpty())
        }
    }

    @Test
    fun `model config selects the model type and annotation`() {
        val configuration =
            create(
                settings(
                    generatorName = "java",
                    modelPackage = "com.example.model",
                    modelConfig = GeneratorSettings.ModelConfig("com.example.NoArg", "java-record"),
                ),
            )

        assertEquals(
            ModelGeneration.Enabled(
                packageName = "com.example.model",
                annotation =
                    QualifiedTypeName.fromConfigurationValue(
                        "com.example.NoArg",
                        "modelConfig.modelAnnotation",
                    ),
                modelType = ModelType.JAVA_RECORD,
            ),
            configuration.models,
        )
    }

    @Test
    fun `native model types generate schemas instead of source models`() {
        val avro =
            create(
                settings(
                    modelPackage = "com.example.avro",
                    modelConfig = GeneratorSettings.ModelConfig(modelType = "avro-specific-record"),
                ),
            )
        val kotlinProtobuf =
            create(
                settings(
                    modelPackage = "com.example.protobuf",
                    modelConfig = GeneratorSettings.ModelConfig(modelType = "protobuf-message"),
                ),
            )
        val javaProtobuf =
            create(
                settings(
                    generatorName = "java",
                    modelPackage = "com.example.protobuf",
                    modelConfig = GeneratorSettings.ModelConfig(modelType = "protobuf-message"),
                ),
            )

        assertEquals(ModelGeneration.Disabled, avro.models)
        assertEquals(
            listOf(SchemaGeneration.NativeAvro(generateSpecificRecords = true, modelPackageName = "com.example.avro")),
            avro.schemas,
        )
        assertEquals(
            listOf(
                SchemaGeneration.NativeProtobuf(
                    ProtobufModelGeneration("com.example.protobuf", ProtobufModelType.KOTLIN),
                ),
            ),
            kotlinProtobuf.schemas,
        )
        assertEquals(
            listOf(
                SchemaGeneration.NativeProtobuf(
                    ProtobufModelGeneration("com.example.protobuf", ProtobufModelType.JAVA),
                ),
            ),
            javaProtobuf.schemas,
        )
    }

    @Test
    fun `schema generators select their schema generation`() {
        assertEquals(
            listOf(
                SchemaGeneration.AvroProjection(packageName = "com.example.schema"),
                SchemaGeneration.NativeAvro(generateSpecificRecords = false, schemaPackageName = "com.example.schema"),
            ),
            create(settings(generatorName = "avro-schema", schemaPackage = "com.example.schema")).schemas,
        )
        assertEquals(
            listOf(SchemaGeneration.NativeProtobuf(schemaPackageName = "com.example.schema")),
            create(settings(generatorName = "protobuf-schema", schemaPackage = "com.example.schema")).schemas,
        )
        assertEquals(
            listOf(SchemaGeneration.JsonSchema(packageName = "com.example.schema")),
            create(settings(generatorName = "json-schema", schemaPackage = "com.example.schema")).schemas,
        )
    }

    @Test
    fun `the output file takes the document format of the generator`() {
        val outputFile = tempDir.resolve("asyncapi").toFile()

        assertEquals(
            DocumentOutput(outputFile, DocumentFormat.JSON),
            create(settings(generatorName = "asyncapi-json", outputFile = outputFile)).output.document,
        )
        assertEquals(
            DocumentOutput(outputFile, DocumentFormat.YAML),
            create(settings(generatorName = "asyncapi-yaml", outputFile = outputFile)).output.document,
        )
        assertEquals(
            DocumentOutput(outputFile, DocumentFormat.YAML),
            create(settings(outputFile = outputFile)).output.document,
        )
    }

    @Test
    fun `the output directory is the single root for all generated artifacts`() {
        val output = create(settings(modelPackage = "com.example.model")).output

        assertEquals(outputDirectory(), output.outputDirectory)
    }

    @Test
    fun `client config maps every client setting`() {
        val configuration =
            create(
                clientSettings(
                    clientConfig(
                        producer =
                            GeneratorSettings.ProducerConfig(
                                enabled = false,
                                additionalPayloadTypes = listOf("string", "byte-array", "string"),
                            ),
                        consumer = GeneratorSettings.ConsumerConfig(enabled = true),
                        topicParameterProperties = mapOf("environment" to "kafka.environment"),
                        validationAnnotations =
                            GeneratorSettings.ValidationAnnotationsConfig(
                                clientContract = "org.springframework.validation.annotation.Validated",
                                payloadParameter = "jakarta.validation.Valid",
                            ),
                    ),
                ),
            )

        val client = requireNotNull(configuration.client)
        assertEquals("com.example.client", client.packageName)
        assertEquals("com.example.model", client.modelPackageName)
        assertEquals(false, client.producer.enabled)
        assertEquals(
            listOf(AdditionalProducerPayloadType.BYTE_ARRAY, AdditionalProducerPayloadType.STRING),
            client.producer.additionalPayloadTypes.toList(),
        )
        assertEquals(true, client.consumer.enabled)
        assertEquals(mapOf("environment" to "kafka.environment"), client.topicParameterProperties.mappings)
        assertEquals(
            "org.springframework.validation.annotation.Validated",
            client.validationAnnotations.clientContract?.value,
        )
        assertEquals("jakarta.validation.Valid", client.validationAnnotations.payloadParameter?.value)
    }

    @Test
    fun `a minimal client config generates Spring Kafka interfaces with producers and consumers`() {
        listOf(
            clientConfig(),
            GeneratorSettings.ClientConfig(),
            clientConfig(
                producer = GeneratorSettings.ProducerConfig(additionalPayloadTypes = emptyList()),
                consumer = GeneratorSettings.ConsumerConfig(),
            ),
        ).forEach { clientConfig ->
            assertEquals(
                ClientGeneration(packageName = "com.example.client", modelPackageName = "com.example.model"),
                create(clientSettings(clientConfig)).client,
            )
        }
    }

    @Test
    fun `client config without client package is ignored with a warning that can be suppressed`() {
        val unused =
            settings(
                clientPackage = null,
                modelPackage = "com.example.model",
                clientConfig = clientConfig(),
            )

        val warned = create(unused)
        val suppressed = create(unused.copy(suppressedWarnings = listOf("unused-client-config")))
        val unknown =
            assertFailsWith<IllegalArgumentException> { create(unused.copy(suppressedWarnings = listOf("everything"))) }

        assertNull(warned.client)
        assertEquals(listOf(ConfigurationWarningId.UNUSED_CLIENT_CONFIG), warned.warnings.map { it.id })
        val printed = warned.formatWarnings()
        assertTrue(">> ${warned.warnings.single().message}" in printed, printed)
        assertTrue("See documentation: ${warned.warnings.single().documentation}" in printed, printed)
        assertTrue(suppressed.warnings.isEmpty())
        assertTrue("everything" in unknown.message.orEmpty())
        assertTrue("unused-client-config" in unknown.message.orEmpty())
    }

    @Test
    fun `client settings are rejected under their public names`() {
        assertRejected(
            "clientConfig is required when clientPackage is configured",
            settings(modelPackage = "com.example.model", clientPackage = "com.example.client"),
        )
        assertRejected(
            "modelPackage is required when clientPackage is configured",
            clientSettings(clientConfig()).copy(modelPackage = null),
        )
        assertRejected(
            "Invalid clientConfig.producer.additionalPayloadTypes 'contract'. Supported values: byte-array, string",
            clientSettings(
                clientConfig(producer = GeneratorSettings.ProducerConfig(additionalPayloadTypes = listOf("contract"))),
            ),
        )
        assertRejected(
            "Spring Kafka client generation requires at least one enabled contract: " +
                "clientConfig.producer.enabled or clientConfig.consumer.enabled",
            clientSettings(
                clientConfig(
                    producer = GeneratorSettings.ProducerConfig(enabled = false),
                    consumer = GeneratorSettings.ConsumerConfig(enabled = false),
                ),
            ),
        )
        assertRejected(
            "clientConfig.topicParameterProperties.environment must be a Spring property name " +
                "without placeholder syntax, for example kafka.environment",
            clientSettings(
                clientConfig(topicParameterProperties = mapOf("environment" to "${'$'}{kafka.environment}")),
            ),
        )
    }

    @Test
    fun `model settings are rejected under their public names`() {
        assertRejected(
            "modelPackage is required when modelConfig is configured",
            settings(modelConfig = GeneratorSettings.ModelConfig(modelAnnotation = "com.example.NoArg")),
        )
        assertRejected(
            "Invalid modelConfig.modelType 'pojo'. Supported values: " +
                "kotlin-data-class, java-class, java-record, avro-specific-record, protobuf-message",
            settings(
                modelPackage = "com.example.model",
                modelConfig = GeneratorSettings.ModelConfig(modelType = "pojo"),
            ),
        )
        assertRejected(
            "modelConfig.modelType 'kotlin-data-class' is not supported when generatorName is java. " +
                "Supported values: java-class, java-record, avro-specific-record, protobuf-message",
            settings(
                generatorName = "java",
                modelPackage = "com.example.model",
                modelConfig = GeneratorSettings.ModelConfig(modelType = "kotlin-data-class"),
            ),
        )
        assertRejected(
            "modelConfig.modelAnnotation is only supported for kotlin-data-class, java-class, and java-record model types",
            settings(
                modelPackage = "com.example.model",
                modelConfig = GeneratorSettings.ModelConfig("com.example.NoArg", "avro-specific-record"),
            ),
        )
        assertRejected(
            "modelConfig.modelAnnotation must be a fully qualified type name, for example com.example.GeneratedPayload",
            settings(
                modelPackage = "com.example.model",
                modelConfig = GeneratorSettings.ModelConfig("GeneratedPayload"),
            ),
        )
    }

    @Test
    fun `package names are rejected under their public names`() {
        assertRejected("modelPackage cannot be empty", settings(modelPackage = " "))
        assertRejected(
            "modelPackage must be a dot-separated package name, for example com.example.model",
            settings(modelPackage = "com.example-model"),
        )
        assertRejected("clientPackage cannot be empty", clientSettings(clientConfig()).copy(clientPackage = " "))
        assertRejected(
            "schemaPackage must be a dot-separated package name, for example com.example.model",
            settings(generatorName = "avro-schema", schemaPackage = "com.example."),
        )
    }

    @Test
    fun `schema generators reject settings they do not use`() {
        val schemaSettings = settings(generatorName = "protobuf-schema", schemaPackage = "com.example.schema")

        assertRejected(
            "schemaPackage is required when generatorName is protobuf-schema",
            schemaSettings.copy(schemaPackage = null),
        )
        assertRejected(
            "modelPackage cannot be configured when generatorName is protobuf-schema",
            schemaSettings.copy(modelPackage = "com.example.model"),
        )
        assertRejected(
            "modelConfig cannot be configured when generatorName is protobuf-schema",
            schemaSettings.copy(modelConfig = GeneratorSettings.ModelConfig(modelAnnotation = "com.example.NoArg")),
        )
    }

    @Test
    fun `document generators reject settings they do not use`() {
        val documentSettings =
            settings(generatorName = "asyncapi-yaml", outputFile = tempDir.resolve("asyncapi.yaml").toFile())

        assertRejected(
            "outputFile is required when generatorName is asyncapi-yaml",
            documentSettings.copy(outputFile = null),
        )
        assertRejected(
            "modelPackage cannot be configured when generatorName is asyncapi-yaml",
            documentSettings.copy(modelPackage = "com.example.model"),
        )
        assertRejected(
            "schemaPackage cannot be configured when generatorName is asyncapi-yaml",
            documentSettings.copy(schemaPackage = "com.example.schema"),
        )
    }

    @Test
    fun `settings without a matching output are rejected`() {
        assertRejected(
            "No generator output is configured. Configure modelPackage, clientPackage with clientConfig, " +
                "schemaPackage with a schema generator, or outputFile.",
            settings(),
        )
        assertRejected(
            "schemaPackage is only supported by schema generator profiles and native Avro or Protobuf models",
            settings(modelPackage = "com.example.model", schemaPackage = "com.example.schema"),
        )
    }

    @Test
    fun `output paths of the wrong kind are rejected`() {
        val file = tempDir.resolve("file").toFile().apply { writeText("") }
        val directory = tempDir.resolve("directory").toFile().apply { mkdirs() }

        assertRejected(
            "outputDirectory must be a directory: $file",
            settings(modelPackage = "com.example.model").copy(outputDirectory = file),
        )
        assertRejected("outputFile must be a file: $directory", settings(outputFile = directory))
    }

    private fun create(settings: GeneratorSettings): GeneratorConfiguration =
        GeneratorConfigurationFactory.create(settings)

    private fun assertRejected(
        expectedMessage: String,
        settings: GeneratorSettings,
    ) {
        val exception = assertFailsWith<IllegalArgumentException> { create(settings) }
        assertEquals(expectedMessage, exception.message)
    }

    private fun outputDirectory(): File = tempDir.resolve("generated").toFile()

    private fun settings(
        generatorName: String = "kotlin",
        outputFile: File? = null,
        modelPackage: String? = null,
        clientPackage: String? = null,
        schemaPackage: String? = null,
        modelConfig: GeneratorSettings.ModelConfig? = null,
        clientConfig: GeneratorSettings.ClientConfig? = null,
    ): GeneratorSettings =
        GeneratorSettings(
            generatorName = generatorName,
            outputDirectory = outputDirectory(),
            outputFile = outputFile,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            schemaPackage = schemaPackage,
            modelConfig = modelConfig,
            clientConfig = clientConfig,
        )

    private fun clientSettings(clientConfig: GeneratorSettings.ClientConfig): GeneratorSettings =
        settings(modelPackage = "com.example.model", clientPackage = "com.example.client", clientConfig = clientConfig)

    private fun clientConfig(
        clientType: String? = "spring-kafka",
        clientContract: String? = "interface",
        producer: GeneratorSettings.ProducerConfig? = null,
        consumer: GeneratorSettings.ConsumerConfig? = null,
        topicParameterProperties: Map<String, String> = emptyMap(),
        validationAnnotations: GeneratorSettings.ValidationAnnotationsConfig? = null,
    ): GeneratorSettings.ClientConfig =
        GeneratorSettings.ClientConfig(
            clientType = clientType,
            clientContract = clientContract,
            producer = producer,
            consumer = consumer,
            topicParameterProperties = topicParameterProperties,
            validationAnnotations = validationAnnotations,
        )
}
