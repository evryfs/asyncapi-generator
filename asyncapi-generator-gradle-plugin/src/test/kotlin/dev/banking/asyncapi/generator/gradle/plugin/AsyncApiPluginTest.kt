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
    fun `should generate kotlin models from valid asyncapi yaml`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()

        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_valid_content_kotlin.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  generatorName.set("kotlin")
                  models {
                      packageName.set("com.example.model")
                  }
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val outputDir = File(projectDir, "build/generated/asyncapi/src/main/kotlin/com/example/model")
        assertTrue(outputDir.exists(), "Output directory should exist")
        assertEquals(outputDir.list()?.isNotEmpty(), true, "Output directory should not be empty")
        GradleTestHelper.copyGeneratedOutputToProjectBuild(File(projectDir, "build/generated/asyncapi"))
    }

    @Test
    fun `should generate kotlin models with groovy nested dsl`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()

        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_valid_content_kotlin.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)

        GradleTestHelper.writeGroovyBuildScript(
            projectDir, """
              plugins { id 'dev.banking.asyncapi.generator' }
              asyncapiGenerate {
                  inputFile = file('specs/api.yaml')
                  codegenOutputDirectory = layout.buildDirectory.dir('generated/asyncapi')
                  generatorName = 'kotlin'
                  models {
                      packageName = 'com.example.groovy.model'
                  }
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val outputDir = File(projectDir, "build/generated/asyncapi/src/main/kotlin/com/example/groovy/model")
        assertTrue(outputDir.exists(), "Output directory should exist")
        assertEquals(outputDir.list()?.isNotEmpty(), true, "Output directory should not be empty")
    }

    @Test
    fun `should allow bundle-only output with no packages`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        File(yamlUrl.toURI()).copyTo(File(projectDir, "api.yaml"))
        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  outputFile.set(layout.buildDirectory.file("bundled.yaml"))
                  generatorName.set("kotlin")
                  // no models/clients/schemas output blocks set
              }"""
        )
        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val bundledFile = File(projectDir, "build/bundled.yaml")
        assertTrue(bundledFile.exists(), "Bundled file should exist")
        assertTrue(bundledFile.length() > 0, "Bundled file should not be empty")
        val codegenRoot = File(projectDir, "build/generated/asyncapi/src/main/kotlin")
        val hasKotlinFiles = codegenRoot.exists() && codegenRoot.walkTopDown().any { it.isFile && it.extension == "kt" }
        assertTrue(!hasKotlinFiles, "No Kotlin files should be generated when packages are not set")
    }

    @Test
    fun `should fail if spring kafka is enabled without package`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        File(yamlUrl.toURI()).copyTo(File(projectDir, "api.yaml"))
        GradleTestHelper.writeBuildScript(projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  generatorName.set("kotlin")
                  models {
                      packageName.set("com.example.model")
                  }
                  clients {
                      springKafka {
                          enabled.set(true)
                      }
                  }
              }""")
        val result = GradleTestHelper.runGradleAndFail(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.FAILED, result.task(":generateAsyncApi")?.outcome)
        assertTrue(
            result.output.contains(
                "clients.springKafka.packageName is required when clients.springKafka is configured",
            ),
        )
    }

    @Test
    fun `should fail if spring kafka mode is invalid`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        File(yamlUrl.toURI()).copyTo(File(projectDir, "api.yaml"))
        GradleTestHelper.writeBuildScript(projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  generatorName.set("kotlin")
                  models {
                      packageName.set("com.example.model")
                  }
                  clients {
                      springKafka {
                          packageName.set("com.example.client")
                          mode.set("basic")
                      }
                  }
              }""")
        val result = GradleTestHelper.runGradleAndFail(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.FAILED, result.task(":generateAsyncApi")?.outcome)
        assertTrue(
            result.output.contains(
                "Invalid clients.springKafka.mode 'basic'. Supported values: full, simple",
            ),
        )
    }

    @Test
    fun `should fail if java model type is invalid`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        File(yamlUrl.toURI()).copyTo(File(projectDir, "api.yaml"))
        GradleTestHelper.writeBuildScript(projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  generatorName.set("java")
                  models {
                      packageName.set("com.example.model")
                      javaModelType.set("data")
                  }
              }""")

        val result = GradleTestHelper.runGradleAndFail(projectDir, "generateAsyncApi")

        assertEquals(TaskOutcome.FAILED, result.task(":generateAsyncApi")?.outcome)
        assertTrue(
            result.output.contains(
                "Invalid models.javaModelType 'data'. Supported values: class, record",
            ),
        )
    }

    @Test
    fun `should generate java record models when java model type is configured`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        File(yamlUrl.toURI()).copyTo(File(projectDir, "api.yaml"))
        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  generatorName.set("java")
                  models {
                      packageName.set("com.example.model")
                      javaModelType.set("record")
                  }
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val generatedRecord =
            File(
                projectDir,
                "build/generated/asyncapi/src/main/java/com/example/model/User.java",
            )
        assertTrue(generatedRecord.readText().contains("public record User("))
    }

    @Test
    fun `should generate models only when no client or schema outputs are configured`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_valid_content_kotlin.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)
        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  generatorName.set("kotlin")
                  models {
                      packageName.set("com.example.model")
                  }
              }"""
        )
        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val modelDir = File(projectDir, "build/generated/asyncapi/src/main/kotlin/com/example/model")
        val clientDir = File(projectDir, "build/generated/asyncapi/src/main/kotlin/com/example/client")
        val schemaDir = File(projectDir, "build/generated/asyncapi/src/main/kotlin/com/example/schema")
        assertTrue(modelDir.exists(), "Model directory should exist")
        assertTrue(!clientDir.exists(), "Client directory should not exist without client output configuration")
        assertTrue(!schemaDir.exists(), "Schema directory should not exist without schema output configuration")
    }

    @Test
    fun `should generate kotlin kafka client from generic kafka yaml`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()

        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  generatorName.set("kotlin")
                  models {
                      packageName.set("com.example.kafka.model")
                  }
                  clients {
                      springKafka {
                          packageName.set("com.example.kafka.client")
                      }
                  }
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val clientDir = File(projectDir, "build/generated/asyncapi/src/main/kotlin/com/example/kafka/client")
        assertTrue(clientDir.exists(), "Client directory should exist")
        GradleTestHelper.copyGeneratedOutputToProjectBuild(File(projectDir, "build/generated/asyncapi"))
    }

    @Test
    fun `should generate kafka client with explicit model package when models are not generated`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()

        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  generatorName.set("kotlin")
                  clients {
                      springKafka {
                          packageName.set("com.example.kafka.client")
                          modelPackageName.set("com.example.kafka.model")
                      }
                  }
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val clientDir = File(projectDir, "build/generated/asyncapi/src/main/kotlin/com/example/kafka/client")
        val modelDir = File(projectDir, "build/generated/asyncapi/src/main/kotlin/com/example/kafka/model")
        assertTrue(clientDir.exists(), "Client directory should exist")
        assertTrue(!modelDir.exists(), "Model directory should not exist when model generation is not configured")
    }

    @Test
    fun `should generate java kafka client from generic kafka yaml`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()

        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  generatorName.set("java")
                  models {
                      packageName.set("com.example.kafka.model")
                  }
                  clients {
                      springKafka {
                          packageName.set("com.example.kafka.client")
                      }
                  }
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val clientDir = File(projectDir, "build/generated/asyncapi/src/main/java/com/example/kafka/client")
        assertTrue(clientDir.exists(), "Client directory should exist")
        GradleTestHelper.copyGeneratedOutputToProjectBuild(File(projectDir, "build/generated/asyncapi"))
    }

    @Test
    fun `should write bundled output file if configured`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        File(yamlUrl.toURI()).copyTo(File(projectDir, "api.yaml"))

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  outputFile.set(layout.buildDirectory.file("bundled.yaml")) // Configured output file
                  
                  generatorName.set("kotlin")
                  models {
                      packageName.set("com.example.bundled")
                  }
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)

        val bundledFile = File(projectDir, "build/bundled.yaml")
        assertTrue(bundledFile.exists(), "Bundled file should exist")
        assertTrue(bundledFile.length() > 0, "Bundled file should not be empty")
    }

    @Test
    fun `should generate avro schema when schema mode is avro projection`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)
        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  resourceOutputDirectory.set(layout.buildDirectory.dir("generated-resources/asyncapi"))
                  generatorName.set("kotlin")
                  models {
                      packageName.set("com.example.avro.model")
                  }
                  schemas {
                      avroProjection {
                          packageName.set("com.example.avro.schema")
                      }
                  }
              }"""
        )
        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val schemaDir = File(projectDir, "build/generated-resources/asyncapi/com/example/avro/schema")
        assertTrue(schemaDir.exists(), "Schema directory should exist")
    }

    @Test
    fun `should generate native avro schema and specific record source`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_native_avro.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)
        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  resourceOutputDirectory.set(layout.buildDirectory.dir("generated-resources/asyncapi"))
                  generatorName.set("kotlin")
                  schemas {
                      nativeAvro {
                          enabled.set(true)
                      }
                  }
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val schemaFile = File(projectDir, "build/generated-resources/asyncapi/com/example/avro/UserCreated.avsc")
        val specificRecordFile = File(projectDir, "build/generated/asyncapi/src/main/java/com/example/avro/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Avro schema output should exist")
        assertTrue(specificRecordFile.exists(), "SpecificRecord source output should exist")
        assertTrue(specificRecordFile.readText().contains("extends org.apache.avro.specific.SpecificRecordBase"))
    }

    @Test
    fun `should generate native protobuf schema`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_native_protobuf.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)
        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  resourceOutputDirectory.set(layout.buildDirectory.dir("generated-resources/asyncapi"))
                  generatorName.set("kotlin")
                  schemas {
                      nativeProtobuf {
                          enabled.set(true)
                      }
                  }
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val schemaFile = File(projectDir, "build/generated-resources/asyncapi/com/example/protobuf/UserCreated.proto")
        val javaMessageFile = File(projectDir, "build/generated/asyncapi/src/main/java/com/example/protobuf/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Protobuf schema output should exist")
        assertTrue(schemaFile.readText().contains("message UserCreated"))
        assertTrue(javaMessageFile.exists(), "Native Protobuf Java message output should exist")
        assertTrue(javaMessageFile.readText().contains("public final class UserCreated"))
    }

    @Test
    fun `should generate native protobuf schema without Java message types when disabled`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_native_protobuf.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)
        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  resourceOutputDirectory.set(layout.buildDirectory.dir("generated-resources/asyncapi"))
                  generatorName.set("kotlin")
                  schemas {
                      nativeProtobuf {
                          enabled.set(true)
                          generateJavaMessageTypes.set(false)
                      }
                  }
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val schemaFile = File(projectDir, "build/generated-resources/asyncapi/com/example/protobuf/UserCreated.proto")
        val javaMessageFile = File(projectDir, "build/generated/asyncapi/src/main/java/com/example/protobuf/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Protobuf schema output should exist")
        assertFalse(javaMessageFile.exists(), "Native Protobuf Java message output should not exist")
    }

    @Test
    fun `should fail if input file is missing`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("missing.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  generatorName.set("kotlin")
                  models {
                      packageName.set("com.example.fail")
                  }
              }"""
        )

        val result = GradleTestHelper.runGradleAndFail(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.FAILED, result.task(":generateAsyncApi")?.outcome)
        assertTrue(result.output.contains("java.io.FileNotFoundException") || result.output.contains("missing.yaml"))
    }

    @Test
    fun `should fail if generator name is invalid`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_valid_content_kotlin.yaml")
        File(yamlUrl.toURI()).copyTo(File(projectDir, "api.yaml"))

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("api.yaml"))
                  codegenOutputDirectory.set(layout.buildDirectory.dir("generated/asyncapi"))
                  generatorName.set("python") // Invalid
                  models {
                      packageName.set("com.example.fail")
                  }
              }"""
        )

        val result = GradleTestHelper.runGradleAndFail(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.FAILED, result.task(":generateAsyncApi")?.outcome)
        assertTrue(result.output.contains("Invalid generatorName 'python'. Supported values: kotlin, java"))
    }
}
