package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.configuration.ClientContract
import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorOutputConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelType
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration
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
                    schemas = listOf(SchemaGeneration.NativeAvro(generateSpecificRecords = false)),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.NativeAvroArtifacts(
                    generateSpecificRecords = false,
                ),
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
    fun `plan includes model annotation on model artifact task when configured`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    models =
                        ModelGeneration.Enabled(
                            packageName = "com.example.model",
                            annotation = "com.example.NoArg",
                        ),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.ModelArtifacts(
                    language = SourceLanguage.KOTLIN,
                    packageName = "com.example.model",
                    annotation = "com.example.NoArg",
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
    fun `plan includes header and Spring Kafka client tasks for Spring Kafka client generation`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    clients = listOf(kafkaClientGeneration()),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.HeaderModelArtifacts(
                    language = SourceLanguage.KOTLIN,
                    packageName = "com.example.client.header",
                ),
                springKafkaClientTask(),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan can disable Kafka header generation`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    clients =
                        listOf(
                            kafkaClientGeneration(
                                headers = ClientGeneration.Headers(enabled = false),
                            ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                springKafkaClientTask(
                    generateHeaders = false,
                ),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes Spring Kafka producer and consumer options on Spring Kafka client task`() {
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
                GenerationTask.HeaderModelArtifacts(
                    language = SourceLanguage.KOTLIN,
                    packageName = "com.example.client.header",
                ),
                springKafkaClientTask(
                    generateProducers = false,
                    generateConsumers = true,
                    clientContract = ClientContract.INTERFACE,
                    validationAnnotations = validationAnnotations,
                ),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan skips Spring Kafka client task when producer and consumer are disabled`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    clients =
                        listOf(
                            kafkaClientGeneration(
                                springKafka =
                                    ClientGeneration.SpringKafka(
                                        producer = ClientGeneration.Producer(enabled = false),
                                        consumer = ClientGeneration.Consumer(enabled = false),
                                    ),
                            ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.HeaderModelArtifacts(
                    language = SourceLanguage.KOTLIN,
                    packageName = "com.example.client.header",
                ),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan skips Kafka tasks when every Kafka capability is disabled`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    clients =
                        listOf(
                            kafkaClientGeneration(
                                headers = ClientGeneration.Headers(enabled = false),
                                springKafka =
                                    ClientGeneration.SpringKafka(
                                        producer = ClientGeneration.Producer(enabled = false),
                                        consumer = ClientGeneration.Consumer(enabled = false),
                                    ),
                            ),
                        ),
                ),
            )

        assertEquals(emptyList(), plan.tasks)
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
                GenerationTask.HeaderModelArtifacts(
                    language = SourceLanguage.JAVA,
                    packageName = "com.example.client.header",
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
            language = language,
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
        headers: ClientGeneration.Headers = ClientGeneration.Headers(),
        springKafka: ClientGeneration.SpringKafka? = ClientGeneration.SpringKafka(),
    ): ClientGeneration.Kafka =
        ClientGeneration.Kafka(
            packageName = clientPackage,
            modelPackageName = modelPackage,
            headers = headers,
            springKafka = springKafka,
        )

    private fun springKafkaClientTask(
        language: SourceLanguage = SourceLanguage.KOTLIN,
        clientPackage: String = "com.example.client",
        modelPackage: String = "com.example.model",
        generateHeaders: Boolean = true,
        generateProducers: Boolean = true,
        generateConsumers: Boolean = true,
        clientContract: ClientContract = ClientContract.INTERFACE,
        validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    ): GenerationTask.SpringKafkaClient =
        GenerationTask.SpringKafkaClient(
            language = language,
            clientPackage = clientPackage,
            modelPackage = modelPackage,
            generateHeaders = generateHeaders,
            generateProducers = generateProducers,
            generateConsumers = generateConsumers,
            clientContract = clientContract,
            validationAnnotations = validationAnnotations,
        )
}
