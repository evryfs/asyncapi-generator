package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.JAVA
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
                    language = JAVA,
                    models =
                        GeneratorConfigurationRequest.Models(
                            packageName = "com.example.model",
                            javaModelType = JavaModelType.RECORD,
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
    fun `create enables Spring Kafka client generation when client package is configured`() {
        val configuration =
            GeneratorConfigurationFactory.create(
                request(
                    models = GeneratorConfigurationRequest.Models(packageName = "com.example.model"),
                    clients =
                        GeneratorConfigurationRequest.Clients(
                            springKafka =
                                GeneratorConfigurationRequest.SpringKafka(
                                    packageName = "com.example.client",
                                ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                ClientGeneration.SpringKafka(
                    packageName = "com.example.client",
                    modelPackageName = "com.example.model",
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
                            springKafka =
                                GeneratorConfigurationRequest.SpringKafka(
                                    packageName = "com.example.client",
                                    modelPackageName = "com.example.external.model",
                                ),
                        ),
                ),
            )

        assertEquals(
            listOf(
                ClientGeneration.SpringKafka(
                    packageName = "com.example.client",
                    modelPackageName = "com.example.external.model",
                ),
            ),
            configuration.clients,
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
                            nativeProtobuf =
                                GeneratorConfigurationRequest.NativeProtobuf(
                                    generateJavaMessageTypes = false,
                                ),
                        ),
                ),
            )

        assertEquals(
            listOf(SchemaGeneration.NativeProtobuf(generateJavaMessageTypes = false)),
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
                                springKafka = GeneratorConfigurationRequest.SpringKafka(),
                            ),
                    ),
                )
            }

        assertEquals(
            "clients.springKafka.packageName is required when clients.springKafka is configured",
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
                                springKafka =
                                    GeneratorConfigurationRequest.SpringKafka(
                                        packageName = "com.example.client",
                                    ),
                            ),
                    ),
                )
            }

        assertEquals(
            "clients.springKafka.modelPackageName is required when models.packageName is not configured",
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
                                javaModelType = JavaModelType.RECORD,
                            ),
                    ),
                )
            }

        assertEquals(
            "models.javaModelType=record is only supported when generatorName is java",
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
            expectedMessage = "clients.springKafka.packageName cannot be empty",
            request =
                request(
                    clients =
                        GeneratorConfigurationRequest.Clients(
                            springKafka =
                                GeneratorConfigurationRequest.SpringKafka(
                                    packageName = " ",
                                    modelPackageName = "com.example.model",
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
                "clients.springKafka.modelPackageName must be a dot-separated package name, " +
                    "for example com.example.model",
            request =
                request(
                    clients =
                        GeneratorConfigurationRequest.Clients(
                            springKafka =
                                GeneratorConfigurationRequest.SpringKafka(
                                    packageName = "com.example.client",
                                    modelPackageName = "com.example.",
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
        language: GeneratorName = GeneratorName.KOTLIN,
        javaSourceOutputDirectory: File = tempDir.resolve("sources").toFile(),
        models: GeneratorConfigurationRequest.Models? = null,
        schemas: GeneratorConfigurationRequest.Schemas = GeneratorConfigurationRequest.Schemas(),
        clients: GeneratorConfigurationRequest.Clients = GeneratorConfigurationRequest.Clients(),
    ): GeneratorConfigurationRequest =
        GeneratorConfigurationRequest(
            language = language,
            sourceOutputDirectory = tempDir.resolve("sources").toFile(),
            javaSourceOutputDirectory = javaSourceOutputDirectory,
            resourceOutputDirectory = tempDir.resolve("resources").toFile(),
            models = models,
            schemas = schemas,
            clients = clients,
        )
}
