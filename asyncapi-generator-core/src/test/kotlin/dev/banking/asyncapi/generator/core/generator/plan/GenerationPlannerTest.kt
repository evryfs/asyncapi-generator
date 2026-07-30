package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.configuration.ClientContract
import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.DocumentFormat
import dev.banking.asyncapi.generator.core.generator.configuration.DocumentOutput
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorOutputConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorProfile
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelType
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals

class GenerationPlannerTest {
    private val planner = GenerationPlanner()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `plan includes bundled document output task when configured`() {
        val outputFile = tempDir.resolve("asyncapi.json").toFile()
        val plan =
            planner.plan(
                GeneratorConfiguration(
                    profile = GeneratorProfile.Document(DocumentFormat.JSON),
                    output =
                        GeneratorOutputConfiguration(
                            sourceOutputDirectory = tempDir.resolve("sources").toFile(),
                            resourceOutputDirectory = tempDir.resolve("resources").toFile(),
                            document =
                                DocumentOutput(
                                    file = outputFile,
                                    format = DocumentFormat.JSON,
                                ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.DocumentArtifact(
                    file = outputFile,
                    format = DocumentFormat.JSON,
                ),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes model and schema artifact tasks when enabled`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    models = ModelGeneration.Enabled(packageName = "com.example.model"),
                    schemas = listOf(SchemaGeneration.AvroProjection(packageName = "com.example.schema")),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.ModelArtifacts(
                    language = SourceLanguage.KOTLIN,
                    packageName = "com.example.model",
                ),
                GenerationTask.AvroSchemaArtifacts(
                    packageName = "com.example.schema",
                ),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes native Avro artifact task when enabled`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    schemas =
                        listOf(
                            SchemaGeneration.NativeAvro(
                                generateSpecificRecords = true,
                                modelPackageName = "com.example.avro",
                            ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.NativeAvroArtifacts(
                    generateSpecificRecords = true,
                    modelPackageName = "com.example.avro",
                ),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes Kafka key models when native Avro models and Spring Kafka contracts are enabled`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    schemas = listOf(SchemaGeneration.NativeAvro(generateSpecificRecords = true)),
                    clients = listOf(kafkaClientGeneration()),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.KafkaKeyModelArtifacts(
                    language = SourceLanguage.KOTLIN,
                    packageName = "com.example.model",
                ),
                springKafkaClientTask(),
                GenerationTask.NativeAvroArtifacts(generateSpecificRecords = true),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes native Protobuf artifact task when enabled`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    schemas = listOf(SchemaGeneration.NativeProtobuf()),
                ),
            )

        assertEquals(
            listOf(GenerationTask.NativeProtobufArtifacts()),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes configured Protobuf model generation`() {
        val models =
            ProtobufModelGeneration(
                packageName = "com.example.protobuf",
                modelType = ProtobufModelType.KOTLIN,
            )
        val plan =
            planner.plan(
                generatorConfiguration(
                    schemas = listOf(SchemaGeneration.NativeProtobuf(models = models)),
                ),
            )

        assertEquals(
            listOf(GenerationTask.NativeProtobufArtifacts(models = models)),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes Kafka key models when native Protobuf models and Spring Kafka contracts are enabled`() {
        val models =
            ProtobufModelGeneration(
                packageName = "com.example.model",
                modelType = ProtobufModelType.KOTLIN,
            )
        val plan =
            planner.plan(
                generatorConfiguration(
                    schemas = listOf(SchemaGeneration.NativeProtobuf(models = models)),
                    clients = listOf(kafkaClientGeneration()),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.KafkaKeyModelArtifacts(
                    language = SourceLanguage.KOTLIN,
                    packageName = "com.example.model",
                ),
                springKafkaClientTask(),
                GenerationTask.NativeProtobufArtifacts(models = models),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes JSON Schema artifact task when enabled`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    schemas = listOf(SchemaGeneration.JsonSchema(packageName = "com.example.schema")),
                ),
            )

        assertEquals(
            listOf(GenerationTask.JsonSchemaArtifacts(packageName = "com.example.schema")),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes model annotation on model artifact task when configured`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    models =
                        ModelGeneration.Enabled(
                            packageName = "com.example.model",
                            annotation =
                                QualifiedTypeName.fromConfigurationValue(
                                    value = "com.example.NoArg",
                                    path = "modelConfig.modelAnnotation",
                                ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.ModelArtifacts(
                    language = SourceLanguage.KOTLIN,
                    packageName = "com.example.model",
                    annotation =
                        QualifiedTypeName.fromConfigurationValue(
                            value = "com.example.NoArg",
                            path = "modelConfig.modelAnnotation",
                        ),
                ),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes Java model type on model artifact task when configured`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    language = SourceLanguage.JAVA,
                    models =
                        ModelGeneration.Enabled(
                            packageName = "com.example.model",
                            javaModelType = JavaModelType.RECORD,
                        ),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.ModelArtifacts(
                    language = SourceLanguage.JAVA,
                    packageName = "com.example.model",
                    javaModelType = JavaModelType.RECORD,
                ),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes Spring Kafka client task for Spring Kafka client generation`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    clients = listOf(kafkaClientGeneration()),
                ),
            )

        assertEquals(
            listOf(springKafkaClientTask()),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes Spring Kafka producer and consumer options on Spring Kafka client task`() {
        val topicParameterProperties =
            TopicParameterProperties.fromConfigurationValues(
                values = mapOf("environment" to "kafka.environment"),
                path = "clients.kafka.springKafka.topicParameterProperties",
            )
        val validationAnnotations =
            ClientValidationAnnotations(
                clientContract =
                    QualifiedTypeName.fromConfigurationValue(
                        value = "org.springframework.validation.annotation.Validated",
                        path = "clients.kafka.springKafka.validationAnnotations.clientContract",
                    ),
                payloadParameter =
                    QualifiedTypeName.fromConfigurationValue(
                        value = "jakarta.validation.Valid",
                        path = "clients.kafka.springKafka.validationAnnotations.payloadParameter",
                    ),
            )
        val plan =
            planner.plan(
                generatorConfiguration(
                    clients =
                        listOf(
                            kafkaClientGeneration(
                                springKafka =
                                    ClientGeneration.SpringKafka(
                                        clientContract = ClientContract.INTERFACE,
                                        topicParameterProperties = topicParameterProperties,
                                        validationAnnotations = validationAnnotations,
                                        producer =
                                            ClientGeneration.Producer(
                                                enabled = false,
                                            ),
                                        consumer = ClientGeneration.Consumer(enabled = true),
                                    ),
                            ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                springKafkaClientTask(
                    generateProducers = false,
                    generateConsumers = true,
                    clientContract = ClientContract.INTERFACE,
                    topicParameterProperties = topicParameterProperties,
                    validationAnnotations = validationAnnotations,
                ),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan uses selected language for language-specific tasks`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    language = SourceLanguage.JAVA,
                    models = ModelGeneration.Enabled(packageName = "com.example.model"),
                    clients =
                        listOf(
                            kafkaClientGeneration(),
                            ClientGeneration.QuarkusKafka(
                                packageName = "com.example.client",
                                modelPackageName = "com.example.model",
                            ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.ModelArtifacts(
                    language = SourceLanguage.JAVA,
                    packageName = "com.example.model",
                ),
                springKafkaClientTask(
                    language = SourceLanguage.JAVA,
                ),
                GenerationTask.QuarkusKafkaClient(
                    language = SourceLanguage.JAVA,
                ),
            ),
            plan.tasks,
        )
    }

    private fun generatorConfiguration(
        language: SourceLanguage = SourceLanguage.KOTLIN,
        models: ModelGeneration = ModelGeneration.Disabled,
        schemas: List<SchemaGeneration> = emptyList(),
        clients: List<ClientGeneration> = emptyList(),
    ): GeneratorConfiguration =
        GeneratorConfiguration(
            profile = GeneratorProfile.Source(language),
            output =
                GeneratorOutputConfiguration(
                    sourceOutputDirectory = tempDir.resolve("sources").toFile(),
                    resourceOutputDirectory = tempDir.resolve("resources").toFile(),
                ),
            models = models,
            schemas = schemas,
            clients = clients,
        )

    private fun kafkaClientGeneration(
        clientPackage: String = "com.example.client",
        modelPackage: String = "com.example.model",
        springKafka: ClientGeneration.SpringKafka? = ClientGeneration.SpringKafka(),
    ): ClientGeneration.Kafka =
        ClientGeneration.Kafka(
            packageName = clientPackage,
            modelPackageName = modelPackage,
            springKafka = springKafka,
        )

    private fun springKafkaClientTask(
        language: SourceLanguage = SourceLanguage.KOTLIN,
        clientPackage: String = "com.example.client",
        modelPackage: String = "com.example.model",
        generateProducers: Boolean = true,
        generateConsumers: Boolean = true,
        clientContract: ClientContract = ClientContract.INTERFACE,
        topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
        validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    ): GenerationTask.SpringKafkaClient =
        GenerationTask.SpringKafkaClient(
            language = language,
            clientPackage = clientPackage,
            modelPackage = modelPackage,
            generateProducers = generateProducers,
            generateConsumers = generateConsumers,
            clientContract = clientContract,
            topicParameterProperties = topicParameterProperties,
            validationAnnotations = validationAnnotations,
        )
}
