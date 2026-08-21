package dev.banking.asyncapi.generator.core.generator.jsonschema

import com.fasterxml.jackson.databind.ObjectMapper
import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonSchemaGeneratorTest {
    private val generator = JsonSchemaGenerator()
    private val fixtures = GenerationInputFixtures()
    private val objectMapper = ObjectMapper()

    @Test
    fun `renders standalone Draft 07 schemas from AsyncAPI Schema Objects`() {
        val input = fixtures.generationInputWithJsonSchemas()

        val result =
            generator.render(
                schemas = input.declaredSchemas,
                multiFormatSchemas = input.multiFormatSchemas,
                packageName = "com.example.schema",
            )

        assertEquals(
            listOf(
                "com/example/schema/Address.schema.json",
                "com/example/schema/MyAccount.schema.json",
            ),
            result.artifacts.map { artifact -> artifact.relativePath },
        )
        assertTrue(result.artifacts.all { artifact -> artifact.kind == GeneratedArtifactKind.SCHEMA })

        val accountSchema =
            objectMapper.readTree(
                result.artifacts.single { artifact -> artifact.relativePath.endsWith("MyAccount.schema.json") }.content,
            )
        assertEquals("http://json-schema.org/draft-07/schema#", accountSchema.path($$"$schema").asText())
        assertEquals("urn:example:my-account", accountSchema.path($$"$id").asText())
        assertEquals(
            listOf("string", "null"),
            accountSchema.path("properties").path("accountId").path("type").map { type -> type.asText() },
        )
        assertTrue(accountSchema.path("properties").path("accountId").has("default"))
        assertTrue(accountSchema.path("properties").path("accountId").path("default").isNull)
        assertEquals(
            "Address.schema.json",
            accountSchema.path("properties").path("address").path($$"$ref").asText(),
        )
        assertTrue(accountSchema.has("if"))
        assertTrue(accountSchema.has("then"))
        assertFalse(accountSchema.has("nullable"))
        assertFalse(accountSchema.has("discriminator"))
        assertFalse(accountSchema.has("deprecated"))
    }

    @Test
    fun `renders native Draft 07 schemas as canonical JSON`() {
        val input = fixtures.generationInputWithNativeJsonSchema()

        val result =
            generator.render(
                schemas = input.declaredSchemas,
                multiFormatSchemas = input.multiFormatSchemas,
                packageName = "com.example.schema",
            )

        val artifact = result.artifacts.single()
        val schema = objectMapper.readTree(artifact.content)
        assertEquals("com/example/schema/MyAccount.schema.json", artifact.relativePath)
        assertEquals("http://json-schema.org/draft-07/schema#", schema.path($$"$schema").asText())
        assertEquals(
            "Address.schema.json",
            schema.path("properties").path("address").path($$"$ref").asText(),
        )
    }

    @Test
    fun `preserves explicit null const from AsyncAPI Schema Objects`() {
        val result =
            generator.render(
                schemas =
                    mapOf(
                        "NullValue" to
                            Schema(
                                type = "null",
                                const = null,
                                constSet = true,
                            ),
                    ),
                multiFormatSchemas = emptyMap(),
                packageName = "com.example.schema",
            )

        val schema = objectMapper.readTree(result.artifacts.single().content)
        assertTrue(schema.has("const"))
        assertTrue(schema.path("const").isNull)
    }

    @Test
    fun `preserves native Draft 07 boolean schemas`() {
        val result =
            generator.render(
                schemas = emptyMap(),
                multiFormatSchemas =
                    mapOf(
                        "AcceptedValue" to
                            MultiFormatSchema(
                                schemaFormat = "application/schema+yaml;version=draft-07",
                                schema = true,
                            ),
                    ),
                packageName = "com.example.schema",
            )

        val artifact = result.artifacts.single()
        assertEquals("com/example/schema/AcceptedValue.schema.json", artifact.relativePath)
        assertEquals("true${System.lineSeparator()}", artifact.content)
    }

    @Test
    fun `renders tuple-form items as a JSON Schema array`() {
        val result =
            generator.render(
                schemas =
                    mapOf(
                        "TupleArray" to
                            Schema(
                                type = "array",
                                tupleItems =
                                    listOf(
                                        SchemaInterface.SchemaInline(Schema(type = "string")),
                                        SchemaInterface.SchemaInline(Schema(type = "integer")),
                                    ),
                            ),
                    ),
                multiFormatSchemas = emptyMap(),
                packageName = "com.example.schema",
            )

        val schema = objectMapper.readTree(result.artifacts.single().content)
        assertEquals("array", schema.path("type").asText())
        assertTrue(schema.path("items").isArray)
        assertEquals(2, schema.path("items").size())
        assertEquals("string", schema.path("items").get(0).path("type").asText())
        assertEquals("integer", schema.path("items").get(1).path("type").asText())
    }

    @Test
    fun `renders tuple-form items with additionalItems`() {
        val result =
            generator.render(
                schemas =
                    mapOf(
                        "TupleWithAdditional" to
                            Schema(
                                type = "array",
                                tupleItems =
                                    listOf(
                                        SchemaInterface.SchemaInline(Schema(type = "string")),
                                    ),
                                additionalItems =
                                    SchemaInterface.SchemaInline(Schema(type = "boolean")),
                            ),
                    ),
                multiFormatSchemas = emptyMap(),
                packageName = "com.example.schema",
            )

        val schema = objectMapper.readTree(result.artifacts.single().content)
        assertTrue(schema.path("items").isArray)
        assertEquals(1, schema.path("items").size())
        assertEquals("string", schema.path("items").get(0).path("type").asText())
        assertEquals("boolean", schema.path("additionalItems").path("type").asText())
    }

    @Test
    fun `preserves single-schema items alongside tuple-form`() {
        val result =
            generator.render(
                schemas =
                    mapOf(
                        "SingleItems" to
                            Schema(
                                type = "array",
                                items =
                                    SchemaInterface.SchemaInline(Schema(type = "string")),
                            ),
                    ),
                multiFormatSchemas = emptyMap(),
                packageName = "com.example.schema",
            )

        val schema = objectMapper.readTree(result.artifacts.single().content)
        assertTrue(schema.path("items").isObject)
        assertEquals("string", schema.path("items").path("type").asText())
    }

    @Test
    fun `rejects invalid native Draft 07 root values`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.InvalidJsonSchema> {
                generator.render(
                    schemas = emptyMap(),
                    multiFormatSchemas =
                        mapOf(
                            "MyAccount" to
                                MultiFormatSchema(
                                    schemaFormat = "application/schema+json;version=draft-07",
                                    schema = "type: object",
                                ),
                        ),
                    packageName = "com.example.schema",
                )
            }

        assertTrue(error.message!!.contains("Draft 07 schema content must be an object or a boolean schema"))
    }
}
