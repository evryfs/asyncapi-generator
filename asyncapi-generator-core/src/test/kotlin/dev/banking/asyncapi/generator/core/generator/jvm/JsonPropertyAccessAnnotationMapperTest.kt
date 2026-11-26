package dev.banking.asyncapi.generator.core.generator.jvm

import dev.banking.asyncapi.generator.core.generator.sourcemodel.PropertyAccess
import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonPropertyAccessAnnotationMapperTest {
    private fun annotation(
        schema: Schema?,
        wireName: String? = null,
    ) = JsonPropertyAccessAnnotationMapper
        .annotationFor(PropertyAccess.of(schema), wireName)
        ?.render(useSiteTarget = "field".takeIf { wireName != null })

    @Test
    fun `maps supported JSON property access modes`() {
        assertEquals(
            "@JsonProperty(access = Access.READ_ONLY)",
            annotation(Schema(type = JsonSchemaType.Single("string"), readOnly = true)),
        )
        assertEquals(
            "@JsonProperty(access = Access.WRITE_ONLY)",
            annotation(Schema(type = JsonSchemaType.Single("string"), writeOnly = true)),
        )
        assertEquals(
            "@JsonProperty(access = Access.READ_WRITE)",
            annotation(Schema(type = JsonSchemaType.Single("string"), readOnly = true, writeOnly = true)),
        )
    }

    @Test
    fun `combines a wire name with an access mode in one annotation`() {
        assertEquals(
            "@field:JsonProperty(\"account-id\")",
            annotation(Schema(type = JsonSchemaType.Single("string")), "account-id"),
        )
        assertEquals(
            "@field:JsonProperty(value = \"account-id\", access = Access.READ_ONLY)",
            annotation(Schema(type = JsonSchemaType.Single("string"), readOnly = true), "account-id"),
        )
    }

    @Test
    fun `imports the access enum only with an access mode`() {
        assertEquals(
            listOf(
                "com.fasterxml.jackson.annotation.JsonProperty",
                "com.fasterxml.jackson.annotation.JsonProperty.Access",
            ),
            JsonPropertyAccessAnnotationMapper.annotationFor(PropertyAccess.READ_ONLY, "account-id")?.imports,
        )
        assertEquals(
            listOf("com.fasterxml.jackson.annotation.JsonProperty"),
            JsonPropertyAccessAnnotationMapper.annotationFor(null, "account-id")?.imports,
        )
    }

    @Test
    fun `omits annotation without a wire name or access mode`() {
        assertNull(annotation(Schema(type = JsonSchemaType.Single("string"))))
        assertNull(annotation(null))
    }
}
