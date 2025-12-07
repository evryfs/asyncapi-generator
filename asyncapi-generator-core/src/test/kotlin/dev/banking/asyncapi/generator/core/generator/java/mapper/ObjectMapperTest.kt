package dev.banking.asyncapi.generator.core.generator.java.mapper

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObjectMapperTest {

    private val allSchemas = GeneratorContext(
        mapOf(
            "SomeObject" to Schema(
                type = "object",
                properties = mapOf("id" to SchemaInterface.SchemaInline(Schema(type = "string")))
            )
        )
    )
    private val rootMapper = JavaTypeMapper(allSchemas)
    private val objectMapper = ObjectMapper()

    @Test
    fun `object with properties should throw exception`() {
        val schema = Schema(
            type = "object",
            properties = mapOf("id" to SchemaInterface.SchemaInline(Schema(type = "string")))
        )

        val exception = assertThrows<IllegalStateException> {
            objectMapper.map(schema, "testObj", rootMapper)
        }

        assertTrue(exception.message!!.contains("bug in the generator pipeline"))
    }

    @Test
    fun `object with no properties or additionalProperties should map to Map(String, Object)`() {
        val schema = Schema(type = "object")
        val result = objectMapper.map(schema, "testMap", rootMapper)
        assertEquals("Map<String, Object>", result)
    }

    @Test
    fun `object with additionalProperties=true should map to Map(String, Object)`() {
        val schema = Schema(type = "object", additionalProperties = SchemaInterface.BooleanSchema(true))
        val result = objectMapper.map(schema, "testMap", rootMapper)
        assertEquals("Map<String, Object>", result)
    }

    @Test
    fun `object with additionalProperties string should map to Map(String, String)`() {
        val additionalPropsSchema = SchemaInterface.SchemaInline(Schema(type = "string"))
        val schema = Schema(type = "object", additionalProperties = additionalPropsSchema)
        val result = objectMapper.map(schema, "testMap", rootMapper)
        assertEquals("Map<String, String>", result)
    }

    @Test
    fun `object with additionalProperties integer should map to Map(String, Integer)`() {
        val additionalPropsSchema = SchemaInterface.SchemaInline(Schema(type = "integer"))
        val schema = Schema(type = "object", additionalProperties = additionalPropsSchema)
        val result = objectMapper.map(schema, "testMap", rootMapper)
        assertEquals("Map<String, Integer>", result)
    }

    @Test
    fun `object with additionalProperties object ref should map to Map(String, SomeObject)`() {
        val additionalPropsSchema = SchemaInterface.SchemaReference(Reference(ref = "#/components/schemas/SomeObject"))
        val schema = Schema(type = "object", additionalProperties = additionalPropsSchema)
        val result = objectMapper.map(schema, "testMap", rootMapper)
        assertEquals("Map<String, SomeObject>", result)
    }

    @Test
    fun `object with additionalProperties=false should not be mapped`() {
        val schema = Schema(type = "object", additionalProperties = SchemaInterface.BooleanSchema(false))
        val result = objectMapper.map(schema, "testMap", rootMapper)

        assertNull(result)
    }
}
