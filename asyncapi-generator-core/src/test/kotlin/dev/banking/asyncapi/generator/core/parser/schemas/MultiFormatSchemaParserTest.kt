package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.context.ParserLoadResourceLimits
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserLoadResourceLimit
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.schemas.SchemaFormat
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MultiFormatSchemaParserTest {
    @TempDir
    lateinit var tempDir: Path


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
            "asyncapi_missing_native_schema_asset.root.components.schemas.MissingNativeAvroSchema.schema.\$ref",
        )
    }

    @Test
    fun `external native schema assets use a source located size limit`() {
        val asset = tempDir.resolve("schema.proto").toFile()
        asset.writeText("message Example {}")
        val file = tempDir.resolve("native-limit.yaml").toFile()
        file.writeText(
            """
            components:
              schemas:
                External:
                  schemaFormat: application/vnd.google.protobuf;version=3
                  schema:
                    ${'$'}ref: ./schema.proto
            """.trimIndent(),
        )
        val limitedContext = AsyncApiContext(
            ParserLoadResourceLimits(
                maxNativeSchemaAssetBytes = 4,
            ),
        )
        val schemaNode = ParserNodeFactory.root(DocumentReaderRegistry.read(file), limitedContext)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("External")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            SchemaParser(limitedContext).parseElement(schemaNode)
        }

        val diagnostic = assertIs<ParserDiagnostic.LoadResourceLimitExceeded>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.LOAD_RESOURCE_LIMIT_EXCEEDED, diagnostic.category)
        assertEquals(ParserLoadResourceLimit.NATIVE_SCHEMA_ASSET_BYTES, diagnostic.limit)
        assertEquals(4L, diagnostic.maximum)
        assertEquals(asset.readBytes().size.toLong(), diagnostic.observed)
        assertEquals("native_limit.root.components.schemas.External.schema.\$ref", diagnostic.path)
        assertEquals(file.canonicalFile, diagnostic.sourceLocation.file.canonicalFile)
    }

    @Test
    fun `external native schema assets are strict utf8 input`() {
        val asset = tempDir.resolve("invalid.proto").toFile()
        asset.writeBytes(byteArrayOf(0xC3.toByte(), 0x28))
        val file = tempDir.resolve("invalid-native-utf8.yaml").toFile()
        file.writeText(
            """
            components:
              schemas:
                External:
                  schemaFormat: application/vnd.google.protobuf;version=3
                  schema:
                    ${'$'}ref: ./invalid.proto
            """.trimIndent(),
        )
        val localContext = AsyncApiContext()
        val schemaNode = ParserNodeFactory.root(DocumentReaderRegistry.read(file), localContext)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("External")

        val error = assertFailsWith<AsyncApiParseException.NativeSchemaAssetReadFailure> {
            SchemaParser(localContext).parseElement(schemaNode)
        }

        assertContains(error.message.orEmpty(), "invalid.proto")
        assertContains(error.message.orEmpty(), "External.schema.\$ref")
    }

    @Test
    fun `repeated native schema assets consume aggregate bytes once`() {
        val asset = tempDir.resolve("shared.proto").toFile()
        asset.writeText("message Shared {}")
        val file = tempDir.resolve("repeated-native.yaml").toFile()
        file.writeText(
            """
            components:
              schemas:
                First:
                  schemaFormat: application/vnd.google.protobuf;version=3
                  schema:
                    ${'$'}ref: ./shared.proto
                Second:
                  schemaFormat: application/vnd.google.protobuf;version=3
                  schema:
                    ${'$'}ref: ./shared.proto
            """.trimIndent(),
        )
        val sourceBytes = file.readBytes().size.toLong()
        val assetBytes = asset.readBytes().size.toLong()
        val exactContext = AsyncApiContext(
            ParserLoadResourceLimits(
                maxAggregateSourceBytes = sourceBytes + assetBytes,
            ),
        )
        val schemasNode = ParserNodeFactory.root(DocumentReaderRegistry.read(file), exactContext)
            .expectObject().required("components")
            .expectObject().required("schemas")

        val schemas = SchemaParser(exactContext).parseMap(schemasNode)

        assertEquals(2, schemas.size)

        val limitedContext = AsyncApiContext(
            ParserLoadResourceLimits(
                maxAggregateSourceBytes = sourceBytes + assetBytes - 1,
            ),
        )
        val limitedSchemasNode = ParserNodeFactory.root(DocumentReaderRegistry.read(file), limitedContext)
            .expectObject().required("components")
            .expectObject().required("schemas")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            SchemaParser(limitedContext).parseMap(limitedSchemasNode)
        }

        val diagnostic = assertIs<ParserDiagnostic.LoadResourceLimitExceeded>(error.diagnostic)
        assertEquals(ParserLoadResourceLimit.AGGREGATE_SOURCE_BYTES, diagnostic.limit)
        assertEquals(sourceBytes + assetBytes, diagnostic.observed)
        assertEquals("repeated_native.root.components.schemas.First.schema.\$ref", diagnostic.path)
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
