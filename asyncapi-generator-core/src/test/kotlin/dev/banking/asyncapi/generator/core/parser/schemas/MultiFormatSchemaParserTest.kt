package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.schemas.SchemaFormat
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MultiFormatSchemaParserTest {

    private val context = AsyncApiContext()
    private val parser = SchemaParser(context)

    @Test
    fun `parse supported AsyncAPI schema format`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_format_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("AsyncApiYamlSchemaFormat")

        val schema = assertIs<SchemaInterface.SchemaInline>(parser.parseElement(schemaNode)).schema

        assertEquals("Supported schema format", schema.title)
        assertEquals("object", schema.type)
    }

    @Test
    fun `parse JSON Schema format as multi-format schema`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_format_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("JsonSchemaDraftSchemaFormat")

        val schema = assertIs<SchemaInterface.MultiFormatSchemaInline>(
            parser.parseElement(schemaNode),
        ).multiFormatSchema
        val rawSchema = assertIs<Map<*, *>>(schema.schema)

        assertEquals("application/schema+json;version=draft-07", schema.schemaFormat)
        assertEquals(SchemaFormat.JSON_SCHEMA_DRAFT_07_JSON, schema.format)
        assertEquals("object", rawSchema["type"])
    }

    @Test
    fun `parse native Avro schema format as multi-format schema`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_format_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("NativeAvroSchemaFormat")

        val schema = assertIs<SchemaInterface.MultiFormatSchemaInline>(
            parser.parseElement(schemaNode),
        ).multiFormatSchema
        val rawSchema = assertIs<Map<*, *>>(schema.schema)

        assertEquals("application/vnd.apache.avro+json;version=1.9.0", schema.schemaFormat)
        assertEquals(SchemaFormat.AVRO_1_9_0_JSON, schema.format)
        assertEquals(true, schema.format.isNativeAvro)
        assertEquals("record", rawSchema["type"])
        assertEquals("UserCreated", rawSchema["name"])
    }

    @Test
    fun `parse native Protobuf schema format as multi-format schema`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_format_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("NativeProtobufSchemaFormat")

        val schema = assertIs<SchemaInterface.MultiFormatSchemaInline>(
            parser.parseElement(schemaNode),
        ).multiFormatSchema
        val rawSchema = assertIs<String>(schema.schema)

        assertEquals("application/vnd.google.protobuf;version=3", schema.schemaFormat)
        assertEquals(SchemaFormat.PROTOBUF_3, schema.format)
        assertEquals(true, schema.format.isNativeProtobuf)
        assertContains(rawSchema, "message UserCreated")
    }

    @Test
    fun `parse external native Avro schema asset as schema text`() {
        val file = TestResources.file("parser/schemas/native-assets/asyncapi_external_native_schema_assets.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("ExternalNativeAvroSchema")

        val schema = assertIs<SchemaInterface.MultiFormatSchemaInline>(
            parser.parseElement(schemaNode),
        ).multiFormatSchema
        val schemaText = assertIs<String>(schema.schema)

        assertEquals("application/vnd.apache.avro+json;version=1.9.0", schema.schemaFormat)
        assertEquals(SchemaFormat.AVRO_1_9_0_JSON, schema.format)
        assertContains(schemaText, "\"type\": \"record\"")
        assertContains(schemaText, "\"name\": \"UserCreated\"")
    }

    @Test
    fun `parse external native Protobuf schema asset as schema text`() {
        val file = TestResources.file("parser/schemas/native-assets/asyncapi_external_native_schema_assets.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("ExternalNativeProtobufSchema")

        val schema = assertIs<SchemaInterface.MultiFormatSchemaInline>(
            parser.parseElement(schemaNode),
        ).multiFormatSchema
        val schemaText = assertIs<String>(schema.schema)

        assertEquals("application/vnd.google.protobuf;version=3", schema.schemaFormat)
        assertEquals(SchemaFormat.PROTOBUF_3, schema.format)
        assertContains(schemaText, "option java_multiple_files = true;")
        assertContains(schemaText, "message UserCreated")
    }

    @Test
    fun `parse external native schema asset reports unreadable file`() {
        val file = TestResources.file("parser/schemas/native-assets/asyncapi_missing_native_schema_asset.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("MissingNativeAvroSchema")

        val error = assertFailsWith<AsyncApiParseException.NativeSchemaAssetReadFailure> {
            parser.parseElement(schemaNode)
        }

        assertContains(error.message.orEmpty(), "Native schema asset 'missing-user-created.avsc' could not be read.")
        assertContains(error.message.orEmpty(), "asyncapi_missing_native_schema_asset.yaml")
        assertContains(
            error.message.orEmpty(),
            "asyncapi_missing_native_schema_asset.root.components.schemas.MissingNativeAvroSchema.schema",
        )
    }

    @Test
    fun `parse unknown schema format reports the format and source`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_format_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("UnknownSchemaFormat")

        val error = assertFailsWith<AsyncApiParseException.UnexpectedSchemaFormat> {
            parser.parseElement(schemaNode)
        }

        assertContains(error.message.orEmpty(), "SchemaFormat: application/unknown is not valid.")
        assertContains(error.message.orEmpty(), "asyncapi_parser_schema_format_invalid.yaml")
        assertContains(
            error.message.orEmpty(),
            "asyncapi_parser_schema_format_invalid.root.components.schemas.UnknownSchemaFormat",
        )
    }

    @Test
    fun `parse non-string schema format reports its expected type and source`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_format_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("NonStringSchemaFormat")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemaNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.BOOLEAN, diagnostic.actualType)
        assertEquals(true, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_schema_format_invalid.root.components.schemas.NonStringSchemaFormat.schemaFormat",
            diagnostic.path,
        )
        assertEquals("root.components.schemas.NonStringSchemaFormat.schemaFormat", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_schema_format_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse multi-format schema missing schema content reports the required member and source`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_format_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("MissingSchema")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemaNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("schema", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals("asyncapi_parser_schema_format_invalid.root.components.schemas.MissingSchema.schema", diagnostic.path)
        assertEquals("root.components.schemas.MissingSchema", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_schema_format_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
