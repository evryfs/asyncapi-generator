package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface.BooleanSchema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface.MultiFormatSchemaInline
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface.SchemaInline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchemaInstanceValidatorTest {
    private val validator = SchemaInstanceValidator(AsyncApiContext())

    @Test
    fun `reports exact nested paths for object and array constraints`() {
        val schema = SchemaInline(
            Schema(
                type = "object",
                required = listOf("name"),
                properties = mapOf(
                    "age" to SchemaInline(Schema(type = "integer", minimum = 18)),
                    "labels" to SchemaInline(
                        Schema(
                            type = "array",
                            uniqueItems = true,
                            items = SchemaInline(Schema(type = "string")),
                        ),
                    ),
                ),
                additionalProperties = BooleanSchema(false),
            ),
        )

        val result = validator.validate(
            schema,
            mapOf(
                "age" to 17.5,
                "labels" to listOf("primary", 2, "primary"),
                "unexpected" to true,
            ),
            "example.payload",
        )

        assertEquals(
            listOf(
                "example.payload",
                "example.payload.age",
                "example.payload.age",
                "example.payload.labels",
                "example.payload.labels[1]",
                "example.payload.unexpected",
            ),
            result.violations.map { it.path },
        )
        assertTrue(result.unsupportedFormats.isEmpty())
    }

    @Test
    fun `evaluates composition and conditional schemas without branch noise`() {
        val string = SchemaInline(Schema(type = "string"))
        val number = SchemaInline(Schema(type = "number"))
        val integer = SchemaInline(Schema(type = "integer"))

        val anyOf = validator.validate(
            SchemaInline(Schema(anyOf = listOf(string, integer))),
            true,
            "example.payload",
        )
        val oneOf = validator.validate(
            SchemaInline(Schema(oneOf = listOf(number, integer))),
            1,
            "example.payload",
        )
        val not = validator.validate(
            SchemaInline(Schema(not = string)),
            "forbidden",
            "example.payload",
        )
        val conditional = validator.validate(
            SchemaInline(
                Schema(
                    ifSchema = SchemaInline(
                        Schema(
                            required = listOf("kind"),
                            properties = mapOf("kind" to SchemaInline(Schema(const = "car", constSet = true))),
                        ),
                    ),
                    thenSchema = SchemaInline(Schema(required = listOf("wheels"))),
                ),
            ),
            mapOf("kind" to "car"),
            "example.payload",
        )

        assertEquals("does not match any schema in 'anyOf'", anyOf.violations.single().message)
        assertEquals("must match exactly one schema in 'oneOf', but matched 2", oneOf.violations.single().message)
        assertEquals("matches a schema prohibited by 'not'", not.violations.single().message)
        assertEquals("is missing required property 'wheels'", conditional.violations.single().message)
    }

    @Test
    fun `distinguishes boolean rejection from an unsupported schema format`() {
        val rejected = validator.validate(BooleanSchema(false), null, "example.payload")
        val unsupported = validator.validate(
            MultiFormatSchemaInline(
                MultiFormatSchema(
                    schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
                    schema = mapOf("type" to "string"),
                ),
            ),
            "value",
            "example.payload",
        )

        assertEquals("is rejected by the boolean schema 'false'", rejected.violations.single().message)
        assertEquals(
            "application/vnd.apache.avro+json;version=1.9.0",
            unsupported.unsupportedFormats.single().schemaFormat,
        )
    }
}
