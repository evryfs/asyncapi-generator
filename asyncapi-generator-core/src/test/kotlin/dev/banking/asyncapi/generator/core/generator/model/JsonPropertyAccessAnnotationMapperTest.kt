package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonPropertyAccessAnnotationMapperTest {
    @Test
    fun `maps supported JSON property access modes`() {
        assertEquals(
            "@JsonProperty(access = Access.READ_ONLY)",
            JsonPropertyAccessAnnotationMapper.annotationFor(Schema(type = "string", readOnly = true)),
        )
        assertEquals(
            "@JsonProperty(access = Access.WRITE_ONLY)",
            JsonPropertyAccessAnnotationMapper.annotationFor(Schema(type = "string", writeOnly = true)),
        )
        assertEquals(
            "@JsonProperty(access = Access.READ_WRITE)",
            JsonPropertyAccessAnnotationMapper.annotationFor(Schema(type = "string", readOnly = true, writeOnly = true)),
        )
    }

    @Test
    fun `omits annotation without an access mode`() {
        assertNull(JsonPropertyAccessAnnotationMapper.annotationFor(Schema(type = "string")))
        assertNull(JsonPropertyAccessAnnotationMapper.annotationFor(null))
    }
}
