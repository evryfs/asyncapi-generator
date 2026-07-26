package dev.banking.asyncapi.generator.gradle.plugin

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncApiPluginTest {
    @Test
    fun `generates Kotlin models through a named execution`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_valid_content_kotlin.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "models",
                executionConfiguration =
                    """
                    generatorName.set("kotlin")
                    inputSpec.set(file("api.yaml"))
                    modelPackage.set("com.example.model")
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateModelsAsyncApi")?.outcome)
        assertTrue(
            File(
                projectDirectory,
                "build/generated/asyncapi/models/com/example/model",
            ).listFiles().orEmpty().isNotEmpty(),
        )
    }

    @Test
    fun `supports the named execution DSL from Groovy`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_valid_content_kotlin.yaml")
        GradleTestHelper.writeGroovyBuildScript(
            projectDirectory,
            """
            plugins { id 'dev.banking.asyncapi.generator' }

            asyncApiGenerator {
                executions {
                    register('models') {
                        generatorName.set('kotlin')
                        inputSpec.set(file('api.yaml'))
                        modelPackage.set('com.example.groovy.model')
                    }
                }
            }
            """,
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(
            File(
                projectDirectory,
                "build/generated/asyncapi/models/com/example/groovy/model",
            ).listFiles().orEmpty().isNotEmpty(),
        )
    }

    @Test
    fun `runs multiple executions with shared client configuration`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml", "first.yaml")
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml", "second.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            """
            plugins { id("dev.banking.asyncapi.generator") }

            asyncApiGenerator {
                clientConfig {
                    clientType.set("spring-kafka")
                    clientContract.set("interface")
                    topicParameterProperties.put("environment", "kafka.environment")
                    validationAnnotations {
                        clientContract.set("org.springframework.validation.annotation.Validated")
                        payloadParameter.set("jakarta.validation.Valid")
                    }
                }

                executions {
                    register("first") {
                        generatorName.set("kotlin")
                        inputSpec.set(file("first.yaml"))
                        modelPackage.set("com.example.first.model")
                        clientPackage.set("com.example.first.client")
                    }
                    register("second") {
                        generatorName.set("kotlin")
                        inputSpec.set(file("second.yaml"))
                        modelPackage.set("com.example.second.model")
                        clientPackage.set("com.example.second.client")
                    }
                }
            }
            """,
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateFirstAsyncApi")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateSecondAsyncApi")?.outcome)
        assertTrue(
            File(
                projectDirectory,
                "build/generated/asyncapi/first/com/example/first/client",
            ).isDirectory,
        )
        assertTrue(
            File(
                projectDirectory,
                "build/generated/asyncapi/second/com/example/second/client",
            ).isDirectory,
        )
    }

    @Test
    fun `compiles generated JVM models and copies generated schema resources`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_valid_content_kotlin.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            """
            import org.gradle.jvm.toolchain.JavaLanguageVersion

            plugins {
                kotlin("jvm") version "2.3.20"
                id("dev.banking.asyncapi.generator")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation("jakarta.validation:jakarta.validation-api:3.1.1")
            }

            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(21))
                }
            }

            kotlin {
                jvmToolchain(21)
            }

            asyncApiGenerator {
                executions {
                    register("kotlinModels") {
                        generatorName.set("kotlin")
                        inputSpec.set(file("api.yaml"))
                        modelPackage.set("com.example.kotlin.model")
                    }
                    register("javaModels") {
                        generatorName.set("java")
                        inputSpec.set(file("api.yaml"))
                        modelPackage.set("com.example.java.model")
                    }
                    register("jsonSchemas") {
                        generatorName.set("json-schema")
                        inputSpec.set(file("api.yaml"))
                        schemaPackage.set("com.example.schema")
                    }
                }
            }
            """,
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "classes")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateKotlinModelsAsyncApi")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateJavaModelsAsyncApi")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateJsonSchemasAsyncApi")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":processResources")?.outcome)
        assertTrue(
            File(
                projectDirectory,
                "build/classes/kotlin/main/com/example/kotlin/model/ValidKotlinUserSchema.class",
            ).isFile,
        )
        assertTrue(
            File(
                projectDirectory,
                "build/classes/java/main/com/example/java/model/ValidKotlinUserSchema.class",
            ).isFile,
        )
        assertTrue(
            File(
                projectDirectory,
                "build/resources/main/com/example/schema/ValidKotlinUserSchema.schema.json",
            ).isFile,
        )
        assertFalse(
            File(projectDirectory, "build/resources/main")
                .walkTopDown()
                .any { file -> file.extension == "java" || file.extension == "kt" },
        )
    }

    @Test
    fun `writes bundled YAML through the AsyncAPI YAML profile`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "document",
                executionConfiguration =
                    """
                    generatorName.set("asyncapi-yaml")
                    inputSpec.set(file("api.yaml"))
                    outputFile.set(layout.buildDirectory.file("bundled.yaml"))
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")
        val bundledFile = File(projectDirectory, "build/bundled.yaml")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(bundledFile.readText().startsWith("asyncapi:"))
    }

    @Test
    fun `writes bundled JSON through the AsyncAPI JSON profile`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "document",
                executionConfiguration =
                    """
                    generatorName.set("asyncapi-json")
                    inputSpec.set(file("api.yaml"))
                    outputFile.set(layout.buildDirectory.file("bundled.json"))
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")
        val bundledFile = File(projectDirectory, "build/bundled.json")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(bundledFile.readText().startsWith("{"))
        assertTrue(bundledFile.readText().contains("\"asyncapi\""))
    }

    @Test
    fun `requires shared client configuration when a client package is configured`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "client",
                executionConfiguration =
                    """
                    generatorName.set("kotlin")
                    inputSpec.set(file("api.yaml"))
                    modelPackage.set("com.example.model")
                    clientPackage.set("com.example.client")
                    """,
            ),
        )

        val result = GradleTestHelper.runGradleAndFail(projectDirectory, "generateAsyncApi")

        assertEquals(TaskOutcome.FAILED, result.task(":generateClientAsyncApi")?.outcome)
        assertTrue(result.output.contains("clientConfig.clientType is required"))
    }

    @Test
    fun `reports an invalid model type`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "models",
                executionConfiguration =
                    """
                    generatorName.set("java")
                    inputSpec.set(file("api.yaml"))
                    modelPackage.set("com.example.model")
                    modelConfig {
                        modelType.set("data")
                    }
                    """,
            ),
        )

        val result = GradleTestHelper.runGradleAndFail(projectDirectory, "generateAsyncApi")

        assertEquals(TaskOutcome.FAILED, result.task(":generateModelsAsyncApi")?.outcome)
        assertTrue(
            result.output.contains(
                "Invalid modelConfig.modelType 'data'. Supported values: kotlin-data-class, java-class, " +
                    "java-record, avro-specific-record, protobuf-message",
            ),
        )
    }

    @Test
    fun `generates Java record models`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "models",
                executionConfiguration =
                    """
                    generatorName.set("java")
                    inputSpec.set(file("api.yaml"))
                    outputDirectory.set(layout.buildDirectory.dir("generated/records"))
                    modelPackage.set("com.example.model")
                    modelConfig {
                        modelType.set("java-record")
                    }
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")
        val generatedRecord =
            File(
                projectDirectory,
                "build/generated/records/com/example/model/User.java",
            )

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(generatedRecord.readText().contains("public record User("))
    }

    @Test
    fun `generates models without client or schema outputs`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_valid_content_kotlin.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "models",
                executionConfiguration =
                    """
                    generatorName.set("kotlin")
                    inputSpec.set(file("api.yaml"))
                    outputDirectory.set(layout.buildDirectory.dir("generated/models"))
                    modelPackage.set("com.example.model")
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(File(projectDirectory, "build/generated/models/com/example/model").isDirectory)
        assertFalse(File(projectDirectory, "build/generated/models/com/example/client").exists())
        assertFalse(File(projectDirectory, "build/generated/models/com/example/schema").exists())
    }

    @Test
    fun `generates Kotlin Spring Kafka clients`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                sharedConfiguration = springKafkaConfiguration(),
                executionName = "client",
                executionConfiguration =
                    """
                    generatorName.set("kotlin")
                    inputSpec.set(file("api.yaml"))
                    outputDirectory.set(layout.buildDirectory.dir("generated/client"))
                    modelPackage.set("com.example.kafka.model")
                    clientPackage.set("com.example.kafka.client")
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(File(projectDirectory, "build/generated/client/com/example/kafka/client").isDirectory)
    }

    @Test
    fun `generates Java Spring Kafka clients`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                sharedConfiguration = springKafkaConfiguration(),
                executionName = "client",
                executionConfiguration =
                    """
                    generatorName.set("java")
                    inputSpec.set(file("api.yaml"))
                    outputDirectory.set(layout.buildDirectory.dir("generated/client"))
                    modelPackage.set("com.example.kafka.model")
                    clientPackage.set("com.example.kafka.client")
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(File(projectDirectory, "build/generated/client/com/example/kafka/client").isDirectory)
    }

    @Test
    fun `writes an optional bundled document with source output`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "models",
                executionConfiguration =
                    """
                    generatorName.set("kotlin")
                    inputSpec.set(file("api.yaml"))
                    outputDirectory.set(layout.buildDirectory.dir("generated/models"))
                    outputFile.set(layout.buildDirectory.file("bundled.yaml"))
                    modelPackage.set("com.example.bundled")
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(File(projectDirectory, "build/bundled.yaml").isFile)
        assertTrue(File(projectDirectory, "build/generated/models/com/example/bundled").isDirectory)
    }

    @Test
    fun `generates Avro schemas through the schema profile`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "schema",
                executionConfiguration =
                    """
                    generatorName.set("avro-schema")
                    inputSpec.set(file("api.yaml"))
                    outputDirectory.set(layout.buildDirectory.dir("generated/schema"))
                    schemaPackage.set("com.example.avro.schema")
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")
        val schemaDirectory =
            File(projectDirectory, "build/generated/schema/com/example/avro/schema")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(schemaDirectory.walkTopDown().any { it.extension == "avsc" })
    }

    @Test
    fun `generates JSON Schemas through the schema profile`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_kafka_complex.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "schema",
                executionConfiguration =
                    """
                    generatorName.set("json-schema")
                    inputSpec.set(file("api.yaml"))
                    outputDirectory.set(layout.buildDirectory.dir("generated/schema"))
                    schemaPackage.set("com.example.json.schema")
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")
        val schemaFile =
            File(
                projectDirectory,
                "build/generated/schema/com/example/json/schema/User.schema.json",
            )

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(schemaFile.isFile)
        assertTrue(schemaFile.readText().contains("\"${'$'}schema\" : \"http://json-schema.org/draft-07/schema#\""))
        assertFalse(
            File(projectDirectory, "build/generated/schema")
                .walkTopDown()
                .any { it.extension == "java" || it.extension == "kt" },
        )
    }

    @Test
    fun `generates native Avro schemas and SpecificRecord sources`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_native_avro.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "avro",
                executionConfiguration =
                    """
                    generatorName.set("java")
                    inputSpec.set(file("api.yaml"))
                    outputDirectory.set(layout.buildDirectory.dir("generated/avro"))
                    modelPackage.set("com.example.avro")
                    schemaPackage.set("com.example.avro")
                    modelConfig {
                        modelType.set("avro-specific-record")
                    }
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")
        val outputDirectory = File(projectDirectory, "build/generated/avro/com/example/avro")
        val schemaFile = File(outputDirectory, "UserCreated.avsc")
        val specificRecordFile = File(outputDirectory, "UserCreated.java")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(schemaFile.isFile)
        assertTrue(specificRecordFile.isFile)
        assertTrue(specificRecordFile.readText().contains("extends org.apache.avro.specific.SpecificRecordBase"))
    }

    @Test
    fun `generates native Protobuf schemas and Java messages`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_native_protobuf.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "protobuf",
                executionConfiguration =
                    """
                    generatorName.set("java")
                    inputSpec.set(file("api.yaml"))
                    outputDirectory.set(layout.buildDirectory.dir("generated/protobuf"))
                    modelPackage.set("com.example.protobuf")
                    schemaPackage.set("com.example.protobuf")
                    modelConfig {
                        modelType.set("protobuf-message")
                    }
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")
        val outputDirectory =
            File(projectDirectory, "build/generated/protobuf/com/example/protobuf")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(File(outputDirectory, "UserCreated.proto").isFile)
        assertTrue(File(outputDirectory, "UserCreated.java").isFile)
    }

    @Test
    fun `generates Protobuf schemas without runtime models`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_native_protobuf.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "protobuf",
                executionConfiguration =
                    """
                    generatorName.set("protobuf-schema")
                    inputSpec.set(file("api.yaml"))
                    outputDirectory.set(layout.buildDirectory.dir("generated/protobuf"))
                    schemaPackage.set("com.example.protobuf")
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")
        val outputDirectory =
            File(projectDirectory, "build/generated/protobuf/com/example/protobuf")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(File(outputDirectory, "UserCreated.proto").isFile)
        assertFalse(File(outputDirectory, "UserCreated.java").exists())
    }

    @Test
    fun `generates Protobuf Java messages and Kotlin DSL`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_native_protobuf.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "protobuf",
                executionConfiguration =
                    """
                    generatorName.set("kotlin")
                    inputSpec.set(file("api.yaml"))
                    outputDirectory.set(layout.buildDirectory.dir("generated/protobuf"))
                    modelPackage.set("com.example.protobuf")
                    schemaPackage.set("com.example.protobuf")
                    modelConfig {
                        modelType.set("protobuf-message")
                    }
                    """,
            ),
        )

        val result = GradleTestHelper.runGradle(projectDirectory, "generateAsyncApi")
        val outputDirectory =
            File(projectDirectory, "build/generated/protobuf/com/example/protobuf")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        assertTrue(File(outputDirectory, "UserCreated.java").isFile)
        assertTrue(File(outputDirectory, "UserCreatedKt.kt").isFile)
    }

    @Test
    fun `fails when an input specification is missing`() {
        val projectDirectory = testProject()
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "models",
                executionConfiguration =
                    """
                    generatorName.set("kotlin")
                    inputSpec.set(file("missing.yaml"))
                    modelPackage.set("com.example.fail")
                    """,
            ),
        )

        val result = GradleTestHelper.runGradleAndFail(projectDirectory, "generateAsyncApi")

        assertEquals(TaskOutcome.FAILED, result.task(":generateModelsAsyncApi")?.outcome)
        assertTrue(result.output.contains("missing.yaml"))
    }

    @Test
    fun `fails when generator name is invalid`() {
        val projectDirectory = testProject()
        copyResource(projectDirectory, "asyncapi_valid_content_kotlin.yaml")
        GradleTestHelper.writeBuildScript(
            projectDirectory,
            kotlinBuildScript(
                executionName = "models",
                executionConfiguration =
                    """
                    generatorName.set("python")
                    inputSpec.set(file("api.yaml"))
                    modelPackage.set("com.example.fail")
                    """,
            ),
        )

        val result = GradleTestHelper.runGradleAndFail(projectDirectory, "generateAsyncApi")

        assertEquals(TaskOutcome.FAILED, result.task(":generateModelsAsyncApi")?.outcome)
        assertTrue(
            result.output.contains(
                "Invalid generatorName 'python'. Supported values: java, kotlin, avro-schema, protobuf-schema, " +
                    "json-schema, asyncapi-yaml, asyncapi-json",
            ),
        )
    }

    private fun testProject(): File = Files.createTempDirectory("gradleTest").toFile()

    private fun copyResource(
        projectDirectory: File,
        resourceName: String,
        targetName: String = "api.yaml",
    ) {
        GradleTestHelper
            .resourceFile(resourceName)
            .copyTo(File(projectDirectory, targetName), overwrite = true)
    }

    private fun springKafkaConfiguration(): String =
        """
        clientConfig {
            clientType.set("spring-kafka")
            clientContract.set("interface")
        }
        """

    private fun kotlinBuildScript(
        executionName: String,
        executionConfiguration: String,
        sharedConfiguration: String = "",
    ): String =
        """
        plugins { id("dev.banking.asyncapi.generator") }

        asyncApiGenerator {
            $sharedConfiguration
            executions {
                register("$executionName") {
                    $executionConfiguration
                }
            }
        }
        """
}
