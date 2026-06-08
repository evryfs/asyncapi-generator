package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.GeneratorOptions
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
                generatorOptions(
                    generateModels = true,
                    generateAvroSchema = true,
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
    fun `plan includes model annotation on model artifact task when configured`() {
        val plan =
            planner.plan(
                generatorOptions(
                    generateModels = true,
                    configOptions = mapOf("model.annotation" to "com.example.NoArg"),
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
    fun `plan includes header and full Spring Kafka client tasks for full client generation`() {
        val plan =
            planner.plan(
                generatorOptions(
                    generateModels = false,
                    generateSpringKafkaClient = true,
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
                generatorOptions(
                    generateModels = false,
                    generateSpringKafkaClient = true,
                    configOptions = mapOf("client.type" to "spring-kafka"),
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
                generatorOptions(
                    generateModels = false,
                    generateSpringKafkaClient = true,
                    configOptions = mapOf("client.type" to "spring-kafka-simple"),
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
                generatorOptions(
                    generateModels = false,
                    generateSpringKafkaClient = true,
                    kafkaTopicsPropertyPrefix = "custom.topics",
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
                generatorOptions(
                    generatorName = GeneratorName.JAVA,
                    generateModels = true,
                    generateSpringKafkaClient = true,
                    generateQuarkusKafkaClient = true,
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
                    generatorOptions(
                        generateModels = false,
                        generateSpringKafkaClient = true,
                        kafkaTopicsPropertyPrefix = "",
                    ),
                )
            }

        assertEquals("kafka.topics.property.prefix cannot be empty", exception.message)
    }

    @Test
    fun `plan rejects unsupported Spring Kafka client type`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                planner.plan(
                    generatorOptions(
                        generateModels = false,
                        generateSpringKafkaClient = true,
                        configOptions = mapOf("client.type" to "springkafka"),
                    ),
                )
            }

        assertEquals(
            "Unsupported client.type 'springkafka'. Supported values: spring-kafka, spring-kafka-simple",
            exception.message,
        )
    }

    private fun generatorOptions(
        generatorName: GeneratorName = GeneratorName.KOTLIN,
        generateModels: Boolean = true,
        generateSpringKafkaClient: Boolean = false,
        generateQuarkusKafkaClient: Boolean = false,
        generateAvroSchema: Boolean = false,
        kafkaTopicsPropertyPrefix: String = "kafka.topics",
        configOptions: Map<String, String> = emptyMap(),
    ): GeneratorOptions =
        GeneratorOptions(
            generatorName = generatorName,
            modelPackage = "com.example.model",
            clientPackage = "com.example.client",
            schemaPackage = "com.example.schema",
            codegenOutputDirectory = tempDir.resolve("sources").toFile(),
            resourceOutputDirectory = tempDir.resolve("resources").toFile(),
            kafkaTopicsPropertyPrefix = kafkaTopicsPropertyPrefix,
            generateModels = generateModels,
            generateSpringKafkaClient = generateSpringKafkaClient,
            generateQuarkusKafkaClient = generateQuarkusKafkaClient,
            generateAvroSchema = generateAvroSchema,
            configOptions = configOptions,
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
