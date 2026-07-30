package dev.banking.asyncapi.generator.core.generator.jsonschema

import com.fasterxml.jackson.databind.ObjectMapper
import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
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
