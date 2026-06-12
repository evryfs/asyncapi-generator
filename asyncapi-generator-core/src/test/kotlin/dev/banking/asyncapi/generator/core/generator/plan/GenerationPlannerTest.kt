package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorOutputConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
                    language = GeneratorName.KOTLIN,
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
                    schemas = listOf(SchemaGeneration.NativeProtobuf),
                ),
            )

        assertEquals(
            listOf(GenerationTask.NativeProtobufArtifacts),
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
                    language = GeneratorName.KOTLIN,
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
                    language = GeneratorName.JAVA,
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
                    language = GeneratorName.JAVA,
                    packageName = "com.example.model",
                    javaModelType = JavaModelType.RECORD,
                ),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes header and full Spring Kafka client tasks for full client generation`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    clients = listOf(springKafkaClientGeneration(clientType = SpringKafkaClientType.FULL)),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.HeaderModelArtifacts(
                    language = GeneratorName.KOTLIN,
                    packageName = "com.example.client.header",
                ),
                springKafkaClientTask(clientType = SpringKafkaClientType.FULL),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan accepts explicit full Spring Kafka client type`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    clients = listOf(springKafkaClientGeneration(clientType = SpringKafkaClientType.FULL)),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.HeaderModelArtifacts(
                    language = GeneratorName.KOTLIN,
                    packageName = "com.example.client.header",
                ),
                springKafkaClientTask(clientType = SpringKafkaClientType.FULL),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan excludes header model task for simple Spring Kafka client generation`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    clients = listOf(springKafkaClientGeneration(clientType = SpringKafkaClientType.SIMPLE)),
                ),
            )

        assertEquals(
            listOf(
                springKafkaClientTask(clientType = SpringKafkaClientType.SIMPLE),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan includes custom topic prefix on Spring Kafka client task`() {
        val plan =
            planner.plan(
                generatorConfiguration(
                    clients =
                        listOf(
                            springKafkaClientGeneration(
                                clientType = SpringKafkaClientType.FULL,
                                topicPropertyPrefix = "custom.topics",
                            ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                GenerationTask.HeaderModelArtifacts(
                    language = GeneratorName.KOTLIN,
                    packageName = "com.example.client.header",
                ),
                springKafkaClientTask(
                    clientType = SpringKafkaClientType.FULL,
                    topicPropertyPrefix = "custom.topics",
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
                    language = GeneratorName.JAVA,
                    models = ModelGeneration.Enabled(packageName = "com.example.model"),
                    clients =
                        listOf(
                            springKafkaClientGeneration(clientType = SpringKafkaClientType.FULL),
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
                    language = GeneratorName.JAVA,
                    packageName = "com.example.model",
                ),
                GenerationTask.HeaderModelArtifacts(
                    language = GeneratorName.JAVA,
                    packageName = "com.example.client.header",
                ),
                springKafkaClientTask(
                    language = GeneratorName.JAVA,
                    clientType = SpringKafkaClientType.FULL,
                ),
                GenerationTask.QuarkusKafkaClient(
                    language = GeneratorName.JAVA,
                ),
            ),
            plan.tasks,
        )
    }

    @Test
    fun `plan rejects Spring Kafka client generation with blank topic prefix`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                planner.plan(
                    generatorConfiguration(
                        clients =
                            listOf(
                                springKafkaClientGeneration(
                                    clientType = SpringKafkaClientType.FULL,
                                    topicPropertyPrefix = "",
                                ),
                            ),
                    ),
                )
            }

        assertEquals("topicPropertyPrefix cannot be empty", exception.message)
    }

    private fun generatorConfiguration(
        language: GeneratorName = GeneratorName.KOTLIN,
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

    private fun springKafkaClientGeneration(
        clientType: SpringKafkaClientType,
        clientPackage: String = "com.example.client",
        modelPackage: String = "com.example.model",
        topicPropertyPrefix: String = "kafka.topics",
    ): ClientGeneration.SpringKafka =
        ClientGeneration.SpringKafka(
            packageName = clientPackage,
            modelPackageName = modelPackage,
            clientType = clientType,
            topicPropertyPrefix = topicPropertyPrefix,
        )

    private fun springKafkaClientTask(
        language: GeneratorName = GeneratorName.KOTLIN,
        clientType: SpringKafkaClientType,
        clientPackage: String = "com.example.client",
        modelPackage: String = "com.example.model",
        topicPropertyPrefix: String = "kafka.topics",
    ): GenerationTask.SpringKafkaClient =
        GenerationTask.SpringKafkaClient(
            language = language,
            clientType = clientType,
            clientPackage = clientPackage,
            modelPackage = modelPackage,
            topicPropertyPrefix = topicPropertyPrefix,
        )
}
