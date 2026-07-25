package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.JAVA
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeneratorConfigurationFactoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `create resolves generator names to source languages`() {
        mapOf(
            GeneratorName.JAVA to SourceLanguage.JAVA,
            GeneratorName.KOTLIN to SourceLanguage.KOTLIN,
        ).forEach { (generatorName, sourceLanguage) ->
            assertEquals(
                sourceLanguage,
                GeneratorConfigurationFactory.create(request(generatorName = generatorName)).sourceLanguage,
            )
        }
    }

    @Test
    fun `create resolves Avro schema generator profile`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    generatorName = GeneratorName.AVRO_SCHEMA,
                    schemaPackageName = "com.example.schema",
                ),
            )

        assertEquals(GeneratorProfile.Schema(SchemaType.AVRO), configuration.profile)
        assertEquals(null, configuration.sourceLanguage)
        assertEquals(
            listOf(
                SchemaGeneration.AvroProjection(packageName = "com.example.schema"),
                SchemaGeneration.NativeAvro(
                    generateSpecificRecords = false,
                    schemaPackageName = "com.example.schema",
                ),
            ),
            configuration.schemas,
        )
    }

    @Test
    fun `create resolves Protobuf schema generator profile`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    generatorName = GeneratorName.PROTOBUF_SCHEMA,
                    schemaPackageName = "com.example.schema",
                ),
            )

        assertEquals(GeneratorProfile.Schema(SchemaType.PROTOBUF), configuration.profile)
        assertEquals(null, configuration.sourceLanguage)
        assertEquals(
            listOf(
                SchemaGeneration.NativeProtobuf(
                    schemaPackageName = "com.example.schema",
                ),
            ),
            configuration.schemas,
        )
    }

    @Test
    fun `create resolves Kotlin Protobuf models from the source generator`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    generatorName = GeneratorName.KOTLIN,
                    models =
                        GeneratorConfigurationRequest.Models(
                            packageName = "com.example.protobuf",
                            modelType = ModelType.PROTOBUF_MESSAGE,
                        ),
                ),
            )

        assertEquals(SourceLanguage.KOTLIN, configuration.sourceLanguage)
        assertEquals(ModelGeneration.Disabled, configuration.models)
        assertEquals(
            listOf(
                SchemaGeneration.NativeProtobuf(
                    models =
                        ProtobufModelGeneration(
                            packageName = "com.example.protobuf",
                            modelType = ProtobufModelType.KOTLIN,
                        ),
                ),
            ),
            configuration.schemas,
        )
    }

    @Test
    fun `create resolves Java Protobuf models from the source generator`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    generatorName = GeneratorName.JAVA,
                    models =
                        GeneratorConfigurationRequest.Models(
                            packageName = "com.example.protobuf",
                            modelType = ModelType.PROTOBUF_MESSAGE,
                        ),
                ),
            )

        assertEquals(SourceLanguage.JAVA, configuration.sourceLanguage)
        assertEquals(
            listOf(
                SchemaGeneration.NativeProtobuf(
                    models = ProtobufModelGeneration(packageName = "com.example.protobuf"),
                ),
            ),
            configuration.schemas,
        )
    }

    @Test
    fun `create enables model generation when model package is configured`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    models =
                        GeneratorConfigurationRequest.Models(
                            packageName = "com.example.model",
                            annotation = "com.example.NoArg",
                        ),
                ),
            )

        assertEquals(
            ModelGeneration.Enabled(
                packageName = "com.example.model",
                annotation = "com.example.NoArg",
                javaModelType = JavaModelType.CLASS,
            ),
            configuration.models,
        )
        assertTrue(configuration.hasConfiguredOutputs())
    }

    @Test
    fun `create enables Java record model generation when configured for Java`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    generatorName = JAVA,
                    models =
                        GeneratorConfigurationRequest.Models(
                            packageName = "com.example.model",
                            modelType = ModelType.JAVA_RECORD,
                        ),
                ),
            )

        assertEquals(
            ModelGeneration.Enabled(
                packageName = "com.example.model",
                javaModelType = JavaModelType.RECORD,
            ),
            configuration.models,
        )
    }

    @Test
    fun `create maps java source output directory when configured`() {
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()

        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    javaSourceOutputDirectory = javaSourceOutputDirectory,
                ),
            )

        assertEquals(javaSourceOutputDirectory, configuration.output.javaSourceOutputDirectory)
    }

    @Test
    fun `create enables Kafka and Spring Kafka client generation when client package is configured`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    models = GeneratorConfigurationRequest.Models(packageName = "com.example.model"),
                    clients =
                        GeneratorConfigurationRequest.Clients(
                            kafka =
                                GeneratorConfigurationRequest.Kafka(
                                    packageName = "com.example.client",
                                    springKafka = GeneratorConfigurationRequest.KafkaSpringKafka(),
                                ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                ClientGeneration.Kafka(
                    packageName = "com.example.client",
                    modelPackageName = "com.example.model",
                    springKafka = ClientGeneration.SpringKafka(),
                ),
            ),
            configuration.clients,
        )
    }

    @Test
    fun `create uses client model package when model generation is not configured`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    clients =
                        GeneratorConfigurationRequest.Clients(
                            kafka =
                                GeneratorConfigurationRequest.Kafka(
                                    packageName = "com.example.client",
                                    modelPackageName = "com.example.external.model",
                                    springKafka = GeneratorConfigurationRequest.KafkaSpringKafka(),
                                ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                ClientGeneration.Kafka(
                    packageName = "com.example.client",
                    modelPackageName = "com.example.external.model",
                    springKafka = ClientGeneration.SpringKafka(),
                ),
            ),
            configuration.clients,
        )
    }

    @Test
    fun `create maps kafka header and spring kafka generation options`() {
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
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    models = GeneratorConfigurationRequest.Models(packageName = "com.example.model"),
                    clients =
                        GeneratorConfigurationRequest.Clients(
                            kafka =
                                GeneratorConfigurationRequest.Kafka(
                                    packageName = "com.example.client",
                                    headers = GeneratorConfigurationRequest.KafkaHeaders(enabled = false),
                                    springKafka =
                                        GeneratorConfigurationRequest.KafkaSpringKafka(
                                            clientContract = ClientContract.INTERFACE,
                                            topicParameterProperties =
                                                mapOf("environment" to "kafka.environment"),
                                            validationAnnotations = validationAnnotations,
                                            producer =
                                                GeneratorConfigurationRequest.KafkaProducer(
                                                    enabled = false,
                                                ),
                                            consumer = GeneratorConfigurationRequest.KafkaConsumer(enabled = true),
                                        ),
                                ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                ClientGeneration.Kafka(
                    packageName = "com.example.client",
                    modelPackageName = "com.example.model",
                    headers = ClientGeneration.Headers(enabled = false),
                    springKafka =
                        ClientGeneration.SpringKafka(
                            clientContract = ClientContract.INTERFACE,
                            topicParameterProperties =
                                TopicParameterProperties.fromConfigurationValues(
                                    values = mapOf("environment" to "kafka.environment"),
                                    path = "clients.kafka.springKafka.topicParameterProperties",
                                ),
                            validationAnnotations = validationAnnotations,
                            producer =
                                ClientGeneration.Producer(
                                    enabled = false,
                                ),
                            consumer = ClientGeneration.Consumer(enabled = true),
                        ),
                ),
            ),
            configuration.clients,
        )
    }

    @Test
    fun `create rejects invalid topic parameter property mappings`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        models = GeneratorConfigurationRequest.Models(packageName = "com.example.model"),
                        clients =
                            GeneratorConfigurationRequest.Clients(
                                kafka =
                                    GeneratorConfigurationRequest.Kafka(
                                        packageName = "com.example.client",
                                        springKafka =
                                            GeneratorConfigurationRequest.KafkaSpringKafka(
                                                topicParameterProperties =
                                                    mapOf("environment" to "${'$'}{kafka.environment}"),
                                            ),
                                    ),
                            ),
                    ),
                )
            }

        assertEquals(
            "clients.kafka.springKafka.topicParameterProperties.environment must be a Spring property name " +
                "without placeholder syntax, for example kafka.environment",
            exception.message,
        )
    }

    @Test
    fun `create enables Avro projection when schema mode is configured`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    schemas =
                        GeneratorConfigurationRequest.Schemas(
                            avroProjection =
                                GeneratorConfigurationRequest.AvroProjection(
                                    packageName = "com.example.schema",
                                ),
                        ),
                ),
            )

        assertEquals(
            listOf(SchemaGeneration.AvroProjection(packageName = "com.example.schema")),
            configuration.schemas,
        )
    }

    @Test
    fun `create enables native Avro when schema mode is configured`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    schemas =
                        GeneratorConfigurationRequest.Schemas(
                            nativeAvro =
                                GeneratorConfigurationRequest.NativeAvro(
                                    generateSpecificRecords = false,
                                ),
                        ),
                ),
            )

        assertEquals(
            listOf(SchemaGeneration.NativeAvro(generateSpecificRecords = false)),
            configuration.schemas,
        )
    }

    @Test
    fun `create enables native Protobuf when schema mode is configured`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    schemas =
                        GeneratorConfigurationRequest.Schemas(
                            nativeProtobuf = GeneratorConfigurationRequest.NativeProtobuf,
                        ),
                ),
            )

        assertEquals(
            listOf(SchemaGeneration.NativeProtobuf()),
            configuration.schemas,
        )
    }

    @Test
    fun `create returns no configured output when no output requests are configured`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(),
            )

        assertEquals(emptyList(), configuration.clients)
        assertEquals(emptyList(), configuration.schemas)
        assertFalse(configuration.hasConfiguredOutputs())
    }

    @Test
    fun `create rejects client type without client package`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        clients =
                            GeneratorConfigurationRequest.Clients(
                                kafka = GeneratorConfigurationRequest.Kafka(),
                            ),
                    ),
                )
            }

        assertEquals(
            "clients.kafka.packageName is required when clients.kafka is configured",
            exception.message,
        )
    }

    @Test
    fun `create rejects client generation without model package`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        clients =
                            GeneratorConfigurationRequest.Clients(
                                kafka =
                                    GeneratorConfigurationRequest.Kafka(
                                        packageName = "com.example.client",
                                        springKafka = GeneratorConfigurationRequest.KafkaSpringKafka(),
                                    ),
                            ),
                    ),
                )
            }

        assertEquals(
            "clients.kafka.modelPackageName is required when models.packageName is not configured",
            exception.message,
        )
    }

    @Test
    fun `create rejects schema mode without schema package`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        schemas =
                            GeneratorConfigurationRequest.Schemas(
                                avroProjection = GeneratorConfigurationRequest.AvroProjection(),
                            ),
                    ),
                )
            }

        assertEquals(
            "schemas.avroProjection.packageName is required when schemas.avroProjection is configured",
            exception.message,
        )
    }

    @Test
    fun `create rejects schema generator without schema package`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(generatorName = GeneratorName.AVRO_SCHEMA),
                )
            }

        assertEquals(
            "schemaPackage is required when generatorName is avro-schema",
            exception.message,
        )
    }

    @Test
    fun `create rejects source configuration for schema generator`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        generatorName = GeneratorName.PROTOBUF_SCHEMA,
                        schemaPackageName = "com.example.schema",
                        models = GeneratorConfigurationRequest.Models(packageName = "com.example.model"),
                    ),
                )
            }

        assertEquals(
            "models cannot be configured when generatorName is protobuf-schema",
            exception.message,
        )
    }

    @Test
    fun `create rejects schema config for schema generator`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        generatorName = GeneratorName.AVRO_SCHEMA,
                        schemaPackageName = "com.example.schema",
                        schemas =
                            GeneratorConfigurationRequest.Schemas(
                                nativeAvro = GeneratorConfigurationRequest.NativeAvro(),
                            ),
                    ),
                )
            }

        assertEquals(
            "schemaConfig cannot be configured when generatorName is avro-schema; " +
                "the generator name already selects the schema type",
            exception.message,
        )
    }

    @Test
    fun `create rejects model annotation without model package`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        models = GeneratorConfigurationRequest.Models(annotation = "com.example.NoArg"),
                    ),
                )
            }

        assertEquals(
            "models.packageName is required when models.annotation is configured",
            exception.message,
        )
    }

    @Test
    fun `create rejects model configuration without model package`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        models = GeneratorConfigurationRequest.Models(),
                    ),
                )
            }

        assertEquals(
            "models.packageName is required when models are configured",
            exception.message,
        )
    }

    @Test
    fun `create rejects Java record model generation for Kotlin`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        models =
                            GeneratorConfigurationRequest.Models(
                                packageName = "com.example.model",
                                modelType = ModelType.JAVA_RECORD,
                            ),
                    ),
                )
            }

        assertEquals(
            "modelConfig.modelType 'java-record' is not supported when generatorName is kotlin. " +
                "Supported values: kotlin-data-class, avro-specific-record, protobuf-message",
            exception.message,
        )
    }

    @Test
    fun `create rejects Kotlin data class model generation for Java`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        generatorName = GeneratorName.JAVA,
                        models =
                            GeneratorConfigurationRequest.Models(
                                packageName = "com.example.model",
                                modelType = ModelType.KOTLIN_DATA_CLASS,
                            ),
                    ),
                )
            }

        assertEquals(
            "modelConfig.modelType 'kotlin-data-class' is not supported when generatorName is java. " +
                "Supported values: java-class, java-record, avro-specific-record, protobuf-message",
            exception.message,
        )
    }

    @Test
    fun `create rejects model annotation for native payload model`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        models =
                            GeneratorConfigurationRequest.Models(
                                packageName = "com.example.model",
                                annotation = "com.example.GeneratedPayload",
                                modelType = ModelType.AVRO_SPECIFIC_RECORD,
                            ),
                    ),
                )
            }

        assertEquals(
            "modelConfig.modelAnnotation is only supported for kotlin-data-class, java-class, " +
                "and java-record model types",
            exception.message,
        )
    }

    @Test
    fun `create rejects empty package names`() {
        assertConfigurationError(
            expectedMessage = "models.packageName cannot be empty",
            request =
                request(
                    models = GeneratorConfigurationRequest.Models(packageName = " "),
                ),
        )
        assertConfigurationError(
            expectedMessage = "schemas.avroProjection.packageName cannot be empty",
            request =
                request(
                    schemas =
                        GeneratorConfigurationRequest.Schemas(
                            avroProjection =
                                GeneratorConfigurationRequest.AvroProjection(
                                    packageName = "",
                                ),
                        ),
                ),
        )
        assertConfigurationError(
            expectedMessage = "clients.kafka.packageName cannot be empty",
            request =
                request(
                    clients =
                        GeneratorConfigurationRequest.Clients(
                            kafka =
                                GeneratorConfigurationRequest.Kafka(
                                    packageName = " ",
                                    modelPackageName = "com.example.model",
                                    springKafka = GeneratorConfigurationRequest.KafkaSpringKafka(),
                                ),
                        ),
                ),
        )
    }

    @Test
    fun `create rejects invalid package names`() {
        assertConfigurationError(
            expectedMessage =
                "models.packageName must be a dot-separated package name, for example com.example.model",
            request =
                request(
                    models = GeneratorConfigurationRequest.Models(packageName = "com.example-model"),
                ),
        )
        assertConfigurationError(
            expectedMessage =
                "clients.kafka.modelPackageName must be a dot-separated package name, " +
                    "for example com.example.model",
            request =
                request(
                    clients =
                        GeneratorConfigurationRequest.Clients(
                            kafka =
                                GeneratorConfigurationRequest.Kafka(
                                    packageName = "com.example.client",
                                    modelPackageName = "com.example.",
                                    springKafka = GeneratorConfigurationRequest.KafkaSpringKafka(),
                                ),
                        ),
                ),
        )
        assertConfigurationError(
            expectedMessage =
                "clients.quarkusKafka.packageName must be a dot-separated package name, " +
                    "for example com.example.model",
            request =
                request(
                    models = GeneratorConfigurationRequest.Models(packageName = "com.example.model"),
                    clients =
                        GeneratorConfigurationRequest.Clients(
                            quarkusKafka =
                                GeneratorConfigurationRequest.QuarkusKafka(
                                    packageName = "1example.client",
                                ),
                        ),
                ),
        )
    }

    private fun assertConfigurationError(
        expectedMessage: String,
        request: GeneratorConfigurationRequest,
    ) {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(request)
            }

        assertEquals(expectedMessage, exception.message)
    }

    private fun request(
        generatorName: GeneratorName = GeneratorName.KOTLIN,
        javaSourceOutputDirectory: File = tempDir.resolve("sources").toFile(),
        schemaPackageName: String? = null,
        models: GeneratorConfigurationRequest.Models? = null,
        schemas: GeneratorConfigurationRequest.Schemas = GeneratorConfigurationRequest.Schemas(),
        clients: GeneratorConfigurationRequest.Clients = GeneratorConfigurationRequest.Clients(),
    ): GeneratorConfigurationRequest =
        GeneratorConfigurationRequest(
            generatorName = generatorName,
            sourceOutputDirectory = tempDir.resolve("sources").toFile(),
            javaSourceOutputDirectory = javaSourceOutputDirectory,
            resourceOutputDirectory = tempDir.resolve("resources").toFile(),
            schemaPackageName = schemaPackageName,
            models = models,
            schemas = schemas,
            clients = clients,
        )
}
