package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.schemas.SchemaFormat
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultiFormatSchemaParserTest : ParserTestSupport() {

    private val parser = SchemaParser(asyncApiContext)

    @Test
    fun `parse supported asyncapi schema format`() {
        val schemaNode = readNode(
            "parser/schemas/asyncapi_parser_schema_format_valid.yaml",
            "components",
            "schemas",
            "AsyncApiYamlSchemaFormat",
        )

        val schema = parser.parseElement(schemaNode)

        assertTrue(schema is SchemaInterface.SchemaInline)
        assertEquals("Supported schema format", schema.schema.title)
        assertEquals("object", schema.schema.type)
    }

    @Test
    fun `parse json schema format as multi format schema`() {
        val schemaNode = readNode(
            "parser/schemas/asyncapi_parser_schema_format_valid.yaml",
            "components",
            "schemas",
            "JsonSchemaDraftSchemaFormat",
        )

        val schema = parser.parseElement(schemaNode)

        assertTrue(schema is SchemaInterface.MultiFormatSchemaInline)
        val rawSchema = schema.multiFormatSchema.schema as Map<*, *>
        assertEquals("application/schema+json;version=draft-07", schema.multiFormatSchema.schemaFormat)
        assertEquals(SchemaFormat.JSON_SCHEMA_DRAFT_07_JSON, schema.multiFormatSchema.format)
        assertEquals("object", rawSchema["type"])
    }

    @Test
    fun `parse native avro schema format as multi format schema`() {
        val schemaNode = readNode(
            "parser/schemas/asyncapi_parser_schema_format_valid.yaml",
            "components",
            "schemas",
            "NativeAvroSchemaFormat",
        )

        val schema = parser.parseElement(schemaNode)

        assertTrue(schema is SchemaInterface.MultiFormatSchemaInline)
        val rawSchema = schema.multiFormatSchema.schema as Map<*, *>
        assertEquals("application/vnd.apache.avro+json;version=1.9.0", schema.multiFormatSchema.schemaFormat)
        assertEquals(SchemaFormat.AVRO_1_9_0_JSON, schema.multiFormatSchema.format)
        assertTrue(schema.multiFormatSchema.format.isNativeAvro)
        assertEquals("record", rawSchema["type"])
        assertEquals("UserCreated", rawSchema["name"])
    }

    @Test
    fun `parse native protobuf schema format as multi format schema`() {
        val schemaNode = readNode(
            "parser/schemas/asyncapi_parser_schema_format_valid.yaml",
            "components",
            "schemas",
            "NativeProtobufSchemaFormat",
        )

        val schema = parser.parseElement(schemaNode)

        assertTrue(schema is SchemaInterface.MultiFormatSchemaInline)
        assertEquals("application/vnd.google.protobuf;version=3", schema.multiFormatSchema.schemaFormat)
        assertEquals(SchemaFormat.PROTOBUF_3, schema.multiFormatSchema.format)
        assertTrue(schema.multiFormatSchema.format.isNativeProtobuf)
        val rawSchema = schema.multiFormatSchema.schema as String
        assertTrue(rawSchema.contains("message UserCreated"))
    }

    @Test
    fun `parse unknown schema format throws UnexpectedSchemaFormat`() {
        val schemaNode = readNode(
            "parser/schemas/asyncapi_parser_schema_format_invalid.yaml",
            "components",
            "schemas",
            "UnknownSchemaFormat",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedSchemaFormat>(
            "SchemaFormat: application/unknown is not valid.",
            "asyncapi_parser_schema_format_invalid.yaml",
            "asyncapi_parser_schema_format_invalid.root.components.schemas.UnknownSchemaFormat",
        ) {
            parser.parseElement(schemaNode)
        }
    }

    @Test
    fun `parse non-string schema format throws UnexpectedValue`() {
        val schemaNode = readNode(
            "parser/schemas/asyncapi_parser_schema_format_invalid.yaml",
            "components",
            "schemas",
            "NonStringSchemaFormat",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected String",
            "found Boolean true",
            "quote the value",
            "asyncapi_parser_schema_format_invalid.yaml",
            "asyncapi_parser_schema_format_invalid.root.components.schemas.NonStringSchemaFormat.schemaFormat",
        ) {
            parser.parseElement(schemaNode)
        }
    }
}
