package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NativeAvroGeneratorTest {
    private val generator = NativeAvroGenerator()
    private val fixtures = GenerationInputFixtures()

    @Test
    fun `render returns schema artifacts for native Avro schemas`() {
        val result = generator.render(fixtures.generationInputWithNativeAvroSchema().multiFormatSchemas)

        val artifact = result.artifacts.single()
        assertEquals("com/example/avro/UserCreated.avsc", artifact.relativePath)
        assertEquals(GeneratedArtifactKind.SCHEMA, artifact.kind)
        assertTrue(artifact.content.contains("\"type\" : \"record\""))
        assertTrue(artifact.content.contains("\"namespace\" : \"com.example.avro\""))
        assertTrue(artifact.content.contains("\"name\" : \"UserCreated\""))
    }

    @Test
    fun `render orders native Avro artifacts by payload name`() {
        val result =
            generator.render(
                linkedMapOf(
                    "UserUpdated" to nativeAvroRecord("UserUpdated"),
                    "UserCreated" to nativeAvroRecord("UserCreated"),
                ),
            )

        assertEquals(
            listOf(
                "com/example/avro/UserCreated.avsc",
                "com/example/avro/UserUpdated.avsc",
            ),
            result.artifacts.map { artifact -> artifact.relativePath },
        )
    }

    @Test
    fun `render returns SpecificRecord source artifacts when enabled`() {
        val result =
            generator.render(
                schemas = fixtures.generationInputWithNativeAvroSchema().multiFormatSchemas,
                generateSpecificRecords = true,
            )

        val sourceArtifact = result.artifacts.single { it.kind == GeneratedArtifactKind.JAVA_SOURCE }
        assertEquals("com/example/avro/UserCreated.java", sourceArtifact.relativePath)
        assertTrue(sourceArtifact.content.contains("package com.example.avro;"))
        assertTrue(sourceArtifact.content.contains("public class UserCreated"))
        assertTrue(sourceArtifact.content.contains("extends org.apache.avro.specific.SpecificRecordBase"))
    }

    @Test
    fun `render ignores non Avro multi format schemas`() {
        val result =
            generator.render(
                mapOf(
                    "UserCreated" to
                        MultiFormatSchema(
                            schemaFormat = "application/vnd.google.protobuf;version=3",
                            schema = "message UserCreated {}",
                        ),
                ),
            )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `render rejects invalid native Avro schemas`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.InvalidNativeAvroSchema> {
                generator.render(
                    mapOf(
                        "UserCreated" to
                            MultiFormatSchema(
                                schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
                                schema =
                                    mapOf(
                                        "type" to "record",
                                        "name" to "UserCreated",
                                        "fields" to "not-a-field-list",
                                    ),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("Native Avro generation failed for payload 'UserCreated'"))
        assertTrue(error.message!!.contains("schema is not valid Avro"))
    }

    private fun nativeAvroRecord(name: String): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
            schema =
                mapOf(
                    "type" to "record",
                    "name" to name,
                    "namespace" to "com.example.avro",
                    "fields" to emptyList<Any>(),
                ),
        )
}
