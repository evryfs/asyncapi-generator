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
                GeneratorConfigurationFactory
                    .create(
                        request(
                            generatorName = generatorName,
                            models = GeneratorConfigurationRequest.Models(packageName = "com.example.model"),
                        ),
                    ).sourceLanguage,
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
    fun `create resolves JSON Schema generator profile`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    generatorName = GeneratorName.JSON_SCHEMA,
                    schemaPackageName = "com.example.schema",
                ),
            )

        assertEquals(GeneratorProfile.Schema(SchemaType.JSON_SCHEMA), configuration.profile)
        assertEquals(null, configuration.sourceLanguage)
        assertEquals(
            listOf(
                SchemaGeneration.JsonSchema(
                    packageName = "com.example.schema",
                ),
            ),
            configuration.schemas,
        )
    }

    @Test
    fun `create resolves AsyncAPI YAML document generator profile`() {
        val outputFile = tempDir.resolve("asyncapi.yaml").toFile()
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    generatorName = GeneratorName.ASYNCAPI_YAML,
                    outputFile = outputFile,
                ),
            )

        assertEquals(GeneratorProfile.Document(DocumentFormat.YAML), configuration.profile)
        assertEquals(
            DocumentOutput(
                file = outputFile,
                format = DocumentFormat.YAML,
            ),
            configuration.output.document,
        )
        assertTrue(configuration.hasConfiguredOutputs())
    }

    @Test
    fun `create resolves AsyncAPI JSON document generator profile`() {
        val outputFile = tempDir.resolve("asyncapi.json").toFile()
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    generatorName = GeneratorName.ASYNCAPI_JSON,
                    outputFile = outputFile,
                ),
            )

        assertEquals(GeneratorProfile.Document(DocumentFormat.JSON), configuration.profile)
        assertEquals(
            DocumentOutput(
                file = outputFile,
                format = DocumentFormat.JSON,
            ),
            configuration.output.document,
        )
    }

    @Test
    fun `create defaults bundled document output to YAML for source profiles`() {
        val outputFile = tempDir.resolve("asyncapi.yaml").toFile()
        val configuration =
            GeneratorConfigurationFactory.create(
                request(outputFile = outputFile),
            )

        assertEquals(
            DocumentOutput(
                file = outputFile,
                format = DocumentFormat.YAML,
            ),
            configuration.output.document,
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
    fun `create carries the configured model package into native Avro model generation`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    models =
                        GeneratorConfigurationRequest.Models(
                            packageName = "com.example.avro",
                            modelType = ModelType.AVRO_SPECIFIC_RECORD,
                        ),
                ),
            )

        assertEquals(ModelGeneration.Disabled, configuration.models)
        assertEquals(
            listOf(
                SchemaGeneration.NativeAvro(
                    generateSpecificRecords = true,
                    modelPackageName = "com.example.avro",
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
                annotation =
                    QualifiedTypeName.fromConfigurationValue(
                        value = "com.example.NoArg",
                        path = "modelConfig.modelAnnotation",
                    ),
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
                    models = GeneratorConfigurationRequest.Models(packageName = "com.example.model"),
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
    fun `create maps spring kafka generation options`() {
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
    fun `create rejects spring kafka configuration without an enabled contract`() {
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
                                                producer =
                                                    GeneratorConfigurationRequest.KafkaProducer(
                                                        enabled = false,
                                                    ),
                                                consumer =
                                                    GeneratorConfigurationRequest.KafkaConsumer(
                                                        enabled = false,
                                                    ),
                                            ),
                                    ),
                            ),
                    ),
                )
            }

        assertEquals(
            "Spring Kafka client generation requires at least one enabled contract: " +
                "producer.enabled or consumer.enabled",
            exception.message,
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
    fun `create rejects source configuration without an activated output`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(request())
            }

        assertEquals(
            "No generator output is configured. Configure modelPackage, clientPackage with clientConfig, " +
                "schemaPackage with a schema generator, or outputFile.",
            exception.message,
        )
    }

    @Test
    fun `create rejects schema package when selected outputs do not generate schemas`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        schemaPackageName = "com.example.schema",
                        models = GeneratorConfigurationRequest.Models(packageName = "com.example.model"),
                    ),
                )
            }

        assertEquals(
            "schemaPackage is only supported by schema generator profiles and native Avro or Protobuf models",
            exception.message,
        )
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
    fun `create rejects schema generation options for schema generator`() {
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
            "schema generation options cannot be configured when generatorName is avro-schema; " +
                "the generator name already selects the schema type",
            exception.message,
        )
    }

    @Test
    fun `create rejects document generator without output file`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(generatorName = GeneratorName.ASYNCAPI_JSON),
                )
            }

        assertEquals(
            "outputFile is required when generatorName is asyncapi-json",
            exception.message,
        )
    }

    @Test
    fun `create rejects model configuration for document generator`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        generatorName = GeneratorName.ASYNCAPI_YAML,
                        outputFile = tempDir.resolve("asyncapi.yaml").toFile(),
                        models = GeneratorConfigurationRequest.Models(packageName = "com.example.model"),
                    ),
                )
            }

        assertEquals(
            "models cannot be configured when generatorName is asyncapi-yaml",
            exception.message,
        )
    }

    @Test
    fun `create rejects schema configuration for document generator`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        generatorName = GeneratorName.ASYNCAPI_JSON,
                        outputFile = tempDir.resolve("asyncapi.json").toFile(),
                        schemaPackageName = "com.example.schema",
                    ),
                )
            }

        assertEquals(
            "schemas cannot be configured when generatorName is asyncapi-json",
            exception.message,
        )
    }

    @Test
    fun `create rejects client configuration for document generator`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationFactory.create(
                    request(
                        generatorName = GeneratorName.ASYNCAPI_YAML,
                        outputFile = tempDir.resolve("asyncapi.yaml").toFile(),
                        clients =
                            GeneratorConfigurationRequest.Clients(
                                kafka =
                                    GeneratorConfigurationRequest.Kafka(
                                        packageName = "com.example.client",
                                        modelPackageName = "com.example.model",
                                    ),
                            ),
                    ),
                )
            }

        assertEquals(
            "clients cannot be configured when generatorName is asyncapi-yaml",
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
    fun `create rejects an empty model annotation`() {
        assertConfigurationError(
            expectedMessage = "modelConfig.modelAnnotation cannot be empty",
            request =
                request(
                    models =
                        GeneratorConfigurationRequest.Models(
                            packageName = "com.example.model",
                            annotation = " ",
                        ),
                ),
        )
    }

    @Test
    fun `create rejects a model annotation without a fully qualified name`() {
        assertConfigurationError(
            expectedMessage =
                "modelConfig.modelAnnotation must be a fully qualified type name, " +
                    "for example com.example.GeneratedPayload",
            request =
                request(
                    models =
                        GeneratorConfigurationRequest.Models(
                            packageName = "com.example.model",
                            annotation = "GeneratedPayload",
                        ),
                ),
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
        outputFile: File? = null,
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
            outputFile = outputFile,
            schemaPackageName = schemaPackageName,
            models = models,
            schemas = schemas,
            clients = clients,
        )
}
