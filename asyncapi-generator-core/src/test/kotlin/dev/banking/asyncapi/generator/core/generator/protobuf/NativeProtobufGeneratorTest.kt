package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.fixtures.GeneratedJavaCompiler
import dev.banking.asyncapi.generator.core.fixtures.GeneratedKotlinCompiler
import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelType
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NativeProtobufGeneratorTest {
    private val generator = NativeProtobufGenerator()
    private val fixtures = GenerationInputFixtures()
    private val javaCompiler = GeneratedJavaCompiler()
    private val kotlinCompiler = GeneratedKotlinCompiler()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `render returns schema artifacts for native Protobuf schemas`() {
        val result = generator.render(fixtures.generationInputWithNativeProtobufSchema().multiFormatSchemas)

        val artifact = result.artifacts.single()
        assertEquals("com/example/protobuf/UserCreated.proto", artifact.relativePath)
        assertEquals(GeneratedArtifactKind.SCHEMA, artifact.kind)
        assertTrue(artifact.content.contains("""syntax = "proto3";"""))
        assertTrue(artifact.content.contains("package com.example.protobuf;"))
        assertTrue(artifact.content.contains("message UserCreated"))
    }

    @Test
    fun `render orders native Protobuf artifacts by payload name`() {
        val result =
            generator.render(
                linkedMapOf(
                    "UserUpdated" to nativeProtobufSchema("UserUpdated"),
                    "UserCreated" to nativeProtobufSchema("UserCreated"),
                ),
            )

        assertEquals(
            listOf(
                "com/example/protobuf/UserCreated.proto",
                "com/example/protobuf/UserUpdated.proto",
            ),
            result.artifacts.map { artifact -> artifact.relativePath },
        )
    }

    @Test
    fun `render returns Java message artifacts when Java Protobuf models are configured`() {
        val result =
            generator.render(
                fixtures.generationInputWithNativeProtobufJavaMessageSchema().multiFormatSchemas,
                models = ProtobufModelGeneration(packageName = "com.example.protobuf"),
            )

        val schemaArtifact = result.artifacts.single { it.relativePath == "com/example/protobuf/UserCreated.proto" }
        val messageArtifact = result.artifacts.single { it.relativePath == "com/example/protobuf/UserCreated.java" }
        val builderArtifact = result.artifacts.single { it.relativePath == "com/example/protobuf/UserCreatedOrBuilder.java" }

        assertEquals(GeneratedArtifactKind.SCHEMA, schemaArtifact.kind)
        assertEquals(GeneratedArtifactKind.JAVA_SOURCE, messageArtifact.kind)
        assertEquals(GeneratedArtifactKind.JAVA_SOURCE, builderArtifact.kind)
        assertTrue(messageArtifact.content.contains("public final class UserCreated"))
        assertTrue(messageArtifact.content.contains("com.google.protobuf.GeneratedMessageV3"))
        assertTrue(builderArtifact.content.contains("public interface UserCreatedOrBuilder"))
        javaCompiler.compile(result.artifacts, tempDir)
    }

    @Test
    fun `render returns Java message artifacts that support Protobuf serialization`() {
        val result =
            generator.render(
                fixtures.generationInputWithNativeProtobufJavaMessageSchema().multiFormatSchemas,
                models = ProtobufModelGeneration(packageName = "com.example.protobuf"),
            )
        val compilation = javaCompiler.compile(result.artifacts, tempDir)

        compilation.classLoader().use { classLoader ->
            val messageClass = classLoader.loadClass("com.example.protobuf.UserCreated")
            val builder = messageClass.getMethod("newBuilder").invoke(null)
            builder.javaClass.getMethod("setUserId", String::class.java).invoke(builder, "user-123")
            builder.javaClass.getMethod("setEmail", String::class.java).invoke(builder, "user@example.com")
            val message = builder.javaClass.getMethod("build").invoke(builder)
            val bytes = messageClass.getMethod("toByteArray").invoke(message) as ByteArray
            val parsed = messageClass.getMethod("parseFrom", ByteArray::class.java).invoke(null, bytes)

            assertEquals("user-123", messageClass.getMethod("getUserId").invoke(parsed))
            assertEquals("user@example.com", messageClass.getMethod("getEmail").invoke(parsed))
        }
    }

    @Test
    fun `render returns Java messages and Kotlin DSL artifacts for Kotlin Protobuf models`() {
        val result =
            generator.render(
                fixtures.generationInputWithNativeProtobufJavaMessageSchema().multiFormatSchemas,
                models =
                    ProtobufModelGeneration(
                        packageName = "com.example.protobuf",
                        modelType = ProtobufModelType.KOTLIN,
                    ),
            )

        val messageArtifact = result.artifacts.single { it.relativePath == "com/example/protobuf/UserCreated.java" }
        val kotlinArtifact = result.artifacts.single { it.relativePath == "com/example/protobuf/UserCreatedKt.kt" }

        assertEquals(GeneratedArtifactKind.JAVA_SOURCE, messageArtifact.kind)
        assertEquals(GeneratedArtifactKind.SOURCE, kotlinArtifact.kind)
        assertTrue(kotlinArtifact.content.contains("public object UserCreatedKt"))
        assertTrue(kotlinArtifact.content.contains("public inline fun userCreated"))
        kotlinCompiler.compile(result.artifacts, tempDir)
    }

    @Test
    fun `render ignores non Protobuf multi format schemas`() {
        val result =
            generator.render(
                mapOf(
                    "UserCreated" to
                        MultiFormatSchema(
                            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
                            schema =
                                mapOf(
                                    "type" to "record",
                                    "name" to "UserCreated",
                                    "fields" to emptyList<Any>(),
                                ),
                        ),
                ),
            )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `render rejects non string native Protobuf schemas`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.InvalidNativeProtobufSchema> {
                generator.render(
                    mapOf(
                        "UserCreated" to
                            MultiFormatSchema(
                                schemaFormat = "application/vnd.google.protobuf;version=3",
                                schema = mapOf("message" to "UserCreated"),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("Native Protobuf generation failed for payload 'UserCreated'"))
        assertTrue(error.message!!.contains("must be provided as .proto text"))
    }

    @Test
    fun `render rejects blank native Protobuf schemas`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.InvalidNativeProtobufSchema> {
                generator.render(
                    mapOf(
                        "UserCreated" to
                            MultiFormatSchema(
                                schemaFormat = "application/vnd.google.protobuf;version=3",
                                schema = "  ",
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("Native Protobuf schema content cannot be blank"))
    }

    @Test
    fun `render rejects model generation when java multiple files is not enabled`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.InvalidNativeProtobufSchema> {
                generator.render(
                    fixtures.generationInputWithNativeProtobufSchema().multiFormatSchemas,
                    models = ProtobufModelGeneration(packageName = "com.example.protobuf"),
                )
            }

        assertTrue(error.message!!.contains("Protobuf model generation requires `option java_multiple_files = true;`"))
    }

    @Test
    fun `render rejects model package that differs from native Protobuf package`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.InvalidNativeProtobufSchema> {
                generator.render(
                    fixtures.generationInputWithNativeProtobufJavaMessageSchema().multiFormatSchemas,
                    models = ProtobufModelGeneration(packageName = "com.example.other"),
                )
            }

        assertTrue(
            error.message!!.contains(
                "Configured model package 'com.example.other' must match the Protobuf Java package " +
                    "'com.example.protobuf'",
            ),
        )
    }

    private fun nativeProtobufSchema(messageName: String): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.google.protobuf;version=3",
            schema =
                """
                syntax = "proto3";

                package com.example.protobuf;

                message $messageName {}
                """.trimIndent(),
        )
}
