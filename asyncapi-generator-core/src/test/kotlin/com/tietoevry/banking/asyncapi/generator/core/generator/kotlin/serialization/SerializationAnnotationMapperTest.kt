package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.serialization

import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SerializationAnnotationMapperTest {

    @Test
    fun `buildAnnotations should return empty if no readWrite flags and includeJsonPropertyName is false`() {
        val mapper = SerializationAnnotationMapper("jackson")
        val schema = Schema(type = "string")

        val result = mapper.buildAnnotations("myField", schema)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `buildAnnotations should generate ReadOnly access`() {
        val mapper = SerializationAnnotationMapper("jackson")
        val schema = Schema(type = "string", readOnly = true)

        val result = mapper.buildAnnotations("myField", schema) // Pass propertyName

        assertEquals(1, result.size)
        assertEquals("@JsonProperty(access = Access.READ_ONLY)", result[0])
    }

    @Test
    fun `buildAnnotations should generate JsonProperty with name when includeJsonPropertyName is true`() {
        val mapper = SerializationAnnotationMapper("jackson", includeJsonPropertyName = true)
        val schema = Schema(type = "string")

        val result = mapper.buildAnnotations("my_json_field", schema)

        assertEquals(1, result.size)
        assertEquals("@JsonProperty(value = \"my_json_field\")", result[0])
    }

    @Test
    fun `buildAnnotations should combine name and access when includeJsonPropertyName is true`() {
        val mapper = SerializationAnnotationMapper("jackson", includeJsonPropertyName = true)
        val schema = Schema(type = "string", readOnly = true)

        val result = mapper.buildAnnotations("my_json_field", schema)

        assertEquals(1, result.size)
        assertTrue(result[0].contains("value = \"my_json_field\""))
        assertTrue(result[0].contains("access = Access.READ_ONLY"))
        assertTrue(result[0].startsWith("@JsonProperty("))
        assertTrue(result[0].endsWith(")"))
    }
}
