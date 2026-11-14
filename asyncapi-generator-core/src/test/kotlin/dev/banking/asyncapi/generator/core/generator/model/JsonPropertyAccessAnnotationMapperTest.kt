package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonPropertyAccessAnnotationMapperTest {
    private fun annotation(schema: Schema?, wireName: String? = null) =
        JsonPropertyAccessAnnotationMapper.annotationFor(schema, wireName, "@field:JsonProperty")

    @Test
    fun `maps supported JSON property access modes`() {
        assertEquals("@JsonProperty(access = Access.READ_ONLY)", annotation(Schema(type = "string", readOnly = true)))
        assertEquals("@JsonProperty(access = Access.WRITE_ONLY)", annotation(Schema(type = "string", writeOnly = true)))
        assertEquals(
            "@JsonProperty(access = Access.READ_WRITE)",
            annotation(Schema(type = "string", readOnly = true, writeOnly = true)),
        )
    }

    @Test
    fun `combines a wire name with an access mode in one annotation`() {
        assertEquals("@field:JsonProperty(\"account-id\")", annotation(Schema(type = "string"), "account-id"))
        assertEquals(
            "@field:JsonProperty(value = \"account-id\", access = Access.READ_ONLY)",
            annotation(Schema(type = "string", readOnly = true), "account-id"),
        )
    }

    @Test
    fun `omits annotation without a wire name or access mode`() {
        assertNull(annotation(Schema(type = "string")))
        assertNull(annotation(null))
    }
}
