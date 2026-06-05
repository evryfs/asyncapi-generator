package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
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
    fun `parse unsupported schema format throws UnsupportedSchemaFormat`() {
        val schemaNode = readNode(
            "parser/schemas/asyncapi_parser_schema_format_invalid.yaml",
            "components",
            "schemas",
            "UnsupportedJsonSchemaFormat",
        )
        assertParseFailure<AsyncApiParseException.UnsupportedSchemaFormat>(
            "SchemaFormat: application/schema+json;version=draft-07 is not supported.",
            "asyncapi_parser_schema_format_invalid.yaml",
            "asyncapi_parser_schema_format_invalid.root.components.schemas.UnsupportedJsonSchemaFormat",
        ) {
            parser.parseElement(schemaNode)
        }
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
