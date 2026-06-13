package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NativeProtobufGeneratorTest {
    private val generator = NativeProtobufGenerator()
    private val fixtures = GenerationInputFixtures()

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
    fun `render returns Java message artifacts for native Protobuf schemas when enabled`() {
        val result =
            generator.render(
                fixtures.generationInputWithNativeProtobufJavaMessageSchema().multiFormatSchemas,
                generateJavaMessageTypes = true,
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
    fun `render rejects Java message generation when java multiple files is not enabled`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.InvalidNativeProtobufSchema> {
                generator.render(
                    fixtures.generationInputWithNativeProtobufSchema().multiFormatSchemas,
                    generateJavaMessageTypes = true,
                )
            }

        assertTrue(error.message!!.contains("Java Protobuf message generation requires `option java_multiple_files = true;`"))
    }
}
