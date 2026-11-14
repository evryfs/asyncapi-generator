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
