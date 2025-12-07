package dev.banking.asyncapi.generator.core.generator.avro.mapper

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AvroTypeMapperTest {

    private val mapper = AvroTypeMapper("com.example.avro")

    @Test
    fun `should map simple primitives`() {
        assertMapping("string", Schema(type = "string"))
        assertMapping("int", Schema(type = "integer"))
        assertMapping("long", Schema(type = "integer", format = "int64"))
        assertMapping("double", Schema(type = "number"))
        assertMapping("boolean", Schema(type = "boolean"))
    }

    @Test
    fun `should map logical types`() {
        assertMapping(
            "{\"type\": \"string\", \"logicalType\": \"uuid\"}",
            Schema(type = "string", format = "uuid")
        )
        assertMapping(
            "{\"type\": \"int\", \"logicalType\": \"date\"}",
            Schema(type = "string", format = "date")
        )
        assertMapping(
            "{\"type\": \"long\", \"logicalType\": \"timestamp-millis\"}",
            Schema(type = "string", format = "date-time")
        )
    }

    @Test
    fun `should map unions for optional fields`() {
        val schema = Schema(type = "string")
        assertEquals("[\"null\", \"string\"]", mapper.mapToAvroType(schema, isOptional = true))
    }

    @Test
    fun `should default to string for unknown types`() {
        assertMapping("string", Schema(type = "unknown"))
        assertMapping("string", null)
    }

    private fun assertMapping(expectedJson: String, schema: Schema?) {
        val actual = mapper.mapToAvroType(schema, isOptional = false)
        val effectiveExpected =
            if (!expectedJson.startsWith("{") && !expectedJson.startsWith("[")) {
                "\"$expectedJson\""
            } else {
                expectedJson
            }
        assertEquals(effectiveExpected, actual)
    }
}
