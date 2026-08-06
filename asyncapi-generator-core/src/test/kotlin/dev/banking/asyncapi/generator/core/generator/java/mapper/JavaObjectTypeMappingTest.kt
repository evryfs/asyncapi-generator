package dev.banking.asyncapi.generator.core.generator.java.mapper

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaObjectTypeMappingTest {

    private val allSchemas = GeneratorContext(
        mapOf(
            "SomeObject" to Schema(
                type = "object",
                properties = mapOf("id" to SchemaInterface.SchemaInline(Schema(type = "string")))
            )
        )
    )
    private val mapper = JavaTypeMapper(allSchemas)

    @Test
    fun `object with properties should throw exception`() {
        val schema = Schema(
            type = "object",
            properties = mapOf("id" to SchemaInterface.SchemaInline(Schema(type = "string")))
        )

        val exception = assertThrows<IllegalStateException> {
            mapper.mapJavaType("testObj", schema)
        }

        assertTrue(exception.message!!.contains("bug in the generator pipeline"))
    }

    @Test
    fun `object with no properties or additionalProperties should map to Map(String, Object)`() {
        val schema = Schema(type = "object")
        val result = mapper.mapJavaType("testMap", schema)
        assertEquals("Map<String, Object>", result)
    }

    @Test
    fun `object with additionalProperties=true should map to Map(String, Object)`() {
        val schema = Schema(type = "object", additionalProperties = SchemaInterface.BooleanSchema(true))
        val result = mapper.mapJavaType("testMap", schema)
        assertEquals("Map<String, Object>", result)
    }

    @Test
    fun `object with additionalProperties string should map to Map(String, String)`() {
        val additionalPropsSchema = SchemaInterface.SchemaInline(Schema(type = "string"))
        val schema = Schema(type = "object", additionalProperties = additionalPropsSchema)
        val result = mapper.mapJavaType("testMap", schema)
        assertEquals("Map<String, String>", result)
    }

    @Test
    fun `object with additionalProperties integer should map to Map(String, Integer)`() {
        val additionalPropsSchema = SchemaInterface.SchemaInline(Schema(type = "integer"))
        val schema = Schema(type = "object", additionalProperties = additionalPropsSchema)
        val result = mapper.mapJavaType("testMap", schema)
        assertEquals("Map<String, Integer>", result)
    }

    @Test
    fun `object with additionalProperties object ref should map to Map(String, SomeObject)`() {
        val additionalPropsSchema = SchemaInterface.SchemaReference(Reference(ref = "#/components/schemas/SomeObject"))
        val schema = Schema(type = "object", additionalProperties = additionalPropsSchema)
        val result = mapper.mapJavaType("testMap", schema)
        assertEquals("Map<String, SomeObject>", result)
    }

    @Test
    fun `object with additionalProperties=false should use the fallback type`() {
        val schema = Schema(type = "object", additionalProperties = SchemaInterface.BooleanSchema(false))
        val result = mapper.mapJavaType("testMap", schema)

        assertEquals("Object", result)
    }
}
