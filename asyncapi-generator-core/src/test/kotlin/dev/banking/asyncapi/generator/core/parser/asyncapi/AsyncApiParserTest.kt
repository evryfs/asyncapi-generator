package dev.banking.asyncapi.generator.core.parser.asyncapi

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AsyncApiParserTest {

    private val context = AsyncApiContext()
    private val parser = AsyncApiParser(context)

    @Test
    fun `parses a complete AsyncAPI document`() {
        val file = TestResources.file("asyncapi_kafka_single_file_example.yaml")
        val document = DocumentReaderRegistry.read(file)
        val rootNode = ParserNodeFactory.root(document, context)
        val result = parser.parse(rootNode)

        assertEquals("3.0.0", result.asyncapi, "AsyncAPI version mismatch")
        assertEquals("Streetlights Kafka API", result.info.title)
        assertEquals("1.0.0", result.info.version)
        assertEquals("application/json", result.defaultContentType)

        assertEquals(3, assertNotNull(result.servers).size)
        assertEquals(8, assertNotNull(result.channels).size)
        assertEquals(4, assertNotNull(result.operations).size)

        val components = assertIs<ComponentInterface.ComponentInline>(result.components).component
        assertEquals(15, assertNotNull(components.schemas).size)
        assertEquals(4, assertNotNull(components.messages).size)
        assertEquals(8, assertNotNull(components.securitySchemes).size)
        assertEquals(2, assertNotNull(components.parameters).size)
        assertEquals(2, assertNotNull(components.operationTraits).size)
        assertEquals(1, assertNotNull(components.messageTraits).size)
    }

    @Test
    fun `parses equivalent yaml and json documents into the same model`() {
        val yamlDocument = DocumentReaderRegistry.read(
            TestResources.file("parser/asyncapi/format-independent.yaml"),
        )
        val jsonDocument = DocumentReaderRegistry.read(
            TestResources.file("parser/asyncapi/format-independent.json"),
        )
        val yamlRoot = ParserNodeFactory.root(yamlDocument, context)
        val jsonRoot = ParserNodeFactory.root(jsonDocument, context)

        val yaml = parser.parse(yamlRoot)
        val json = parser.parse(jsonRoot)

        assertEquals("urn:com:example:format-independent", yaml.id)
        assertEquals("application/json", yaml.defaultContentType)
        assertEquals(yaml, json)
    }

    @Test
    fun `yaml and json reject unknown root members`() {
        val sources = listOf(
            DocumentSource(
                id = "unknown-yaml",
                file = File("unknown.yaml").canonicalFile,
                content =
                    """
                    asyncapi: 3.0.0
                    info:
                      title: Unknown member
                      version: 1.0.0
                    unexpected: true
                    """.trimIndent(),
                format = DocumentFormat.YAML,
            ),
            DocumentSource(
                id = "unknown-json",
                file = File("unknown.json").canonicalFile,
                content =
                    """
                    {
                      "asyncapi": "3.0.0",
                      "info": {"title": "Unknown member", "version": "1.0.0"},
                      "unexpected": true
                    }
                    """.trimIndent(),
                format = DocumentFormat.JSON,
            ),
        )

        sources.forEach { source ->
            val sourceContext = AsyncApiContext()
            val document = DocumentReaderRegistry.read(source)
            val root = ParserNodeFactory.root(document, sourceContext)

            val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
                AsyncApiParser(sourceContext).parse(root)
            }
            val diagnostic = assertIs<ParserDiagnostic.UnexpectedObjectMember>(error.diagnostic)

            assertEquals("unexpected", diagnostic.memberName)
            assertEquals("root.unexpected", diagnostic.sourceLocation.path)
            assertEquals(source.file.name, diagnostic.sourceLocation.file.name)
        }
    }

    @Test
    fun `yaml and json accept an explicit null channel address`() {
        val sources = listOf(
            DocumentSource(
                id = "nullable-channel-yaml",
                file = File("nullable-channel.yaml").canonicalFile,
                content =
                    """
                    asyncapi: 3.0.0
                    info:
                      title: Nullable channel
                      version: 1.0.0
                    channels:
                      dynamic:
                        address: null
                    """.trimIndent(),
                format = DocumentFormat.YAML,
            ),
            DocumentSource(
                id = "nullable-channel-json",
                file = File("nullable-channel.json").canonicalFile,
                content =
                    """
                    {
                      "asyncapi": "3.0.0",
                      "info": {"title": "Nullable channel", "version": "1.0.0"},
                      "channels": {"dynamic": {"address": null}}
                    }
                    """.trimIndent(),
                format = DocumentFormat.JSON,
            ),
        )

        sources.forEach { source ->
            val sourceContext = AsyncApiContext()
            val document = DocumentReaderRegistry.read(source)
            val root = ParserNodeFactory.root(document, sourceContext)

            val parsed = AsyncApiParser(sourceContext).parse(root)
            val channel = assertIs<ChannelInterface.ChannelInline>(parsed.channels?.get("dynamic")).channel

            assertNull(channel.address)
        }
    }

    @Test
    fun `yaml and json require operation channel`() {
        val sources = listOf(
            DocumentSource(
                id = "missing-operation-channel-yaml",
                file = File("missing-operation-channel.yaml").canonicalFile,
                content =
                    """
                    asyncapi: 3.0.0
                    info:
                      title: Missing operation channel
                      version: 1.0.0
                    operations:
                      sendMessage:
                        action: send
                    """.trimIndent(),
                format = DocumentFormat.YAML,
            ),
            DocumentSource(
                id = "missing-operation-channel-json",
                file = File("missing-operation-channel.json").canonicalFile,
                content =
                    """
                    {
                      "asyncapi": "3.0.0",
                      "info": {"title": "Missing operation channel", "version": "1.0.0"},
                      "operations": {"sendMessage": {"action": "send"}}
                    }
                    """.trimIndent(),
                format = DocumentFormat.JSON,
            ),
        )

        sources.forEach { source ->
            val sourceContext = AsyncApiContext()
            val document = DocumentReaderRegistry.read(source)
            val root = ParserNodeFactory.root(document, sourceContext)

            val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
                AsyncApiParser(sourceContext).parse(root)
            }
            val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

            assertEquals("channel", diagnostic.memberName)
            assertEquals("root.operations.sendMessage", diagnostic.sourceLocation.path)
            assertEquals(source.file.name, diagnostic.sourceLocation.file.name)
        }
    }

    @Test
    fun `yaml and json ignore Reference Object siblings`() {
        val sources = listOf(
            DocumentSource(
                id = "reference-siblings-yaml",
                file = File("reference-siblings.yaml").canonicalFile,
                content =
                    """
                    asyncapi: 3.0.0
                    info:
                      title: Reference siblings
                      version: 1.0.0
                    channels:
                      source: {}
                      alias:
                        ${'$'}ref: '#/channels/source'
                        unexpected: ignored
                    """.trimIndent(),
                format = DocumentFormat.YAML,
            ),
            DocumentSource(
                id = "reference-siblings-json",
                file = File("reference-siblings.json").canonicalFile,
                content =
                    """
                    {
                      "asyncapi": "3.0.0",
                      "info": {"title": "Reference siblings", "version": "1.0.0"},
                      "channels": {
                        "source": {},
                        "alias": {"${'$'}ref": "#/channels/source", "unexpected": "ignored"}
                      }
                    }
                    """.trimIndent(),
                format = DocumentFormat.JSON,
            ),
        )

        sources.forEach { source ->
            val sourceContext = AsyncApiContext()
            val document = DocumentReaderRegistry.read(source)
            val root = ParserNodeFactory.root(document, sourceContext)

            val parsed = AsyncApiParser(sourceContext).parse(root)
            val reference = assertIs<ChannelInterface.ChannelReference>(parsed.channels?.get("alias")).reference

            assertEquals("#/channels/source", reference.ref)
        }
    }

    @Test
    fun `parsed schema is registered in model repository`() {
        val file = TestResources.file("asyncapi_kafka_single_file_example.yaml")
        val document = DocumentReaderRegistry.read(file)
        val rootNode = ParserNodeFactory.root(document, context)
        val asyncApi = parser.parse(rootNode)
        val components = assertIs<ComponentInterface.ComponentInline>(asyncApi.components).component
        val schemas = assertNotNull(components.schemas)

        val lightMeasuredPayloadSchema =
            assertIs<SchemaInterface.SchemaInline>(schemas["lightMeasuredPayload"]).schema
        val referencedSchema =
            assertIs<SchemaInterface.SchemaReference>(schemas["referencedSchema"]).reference
        val modelRepositoryModels = context.modelRepository.getModelsByInstance()

        assertNotNull(
            modelRepositoryModels[lightMeasuredPayloadSchema],
            "lightMeasuredPayload Schema should be registered"
        )
        assertNotNull(
            modelRepositoryModels[referencedSchema],
            "referencedSchema Reference should be registered"
        )
        val properties = assertNotNull(lightMeasuredPayloadSchema.properties)
        val nestedLumensSchema = assertIs<SchemaInterface.SchemaInline>(properties["lumens"]).schema
        assertNotNull(
            modelRepositoryModels[nestedLumensSchema],
            "Nested lumens schema should be registered"
        )
        val modelRepositoryPaths = context.modelRepository.getModelsByPath()
        val expectedPathForLightMeasuredPayload =
            "${context.sourceRepository.getCurrentFile().nameWithoutExtension}.root.components.schemas.lightMeasuredPayload"
        assertTrue(
            modelRepositoryPaths.containsKey(expectedPathForLightMeasuredPayload),
            "Path for lightMeasuredPayload should be registered"
        )
        val expectedPathForReferencedSchema =
            "${context.sourceRepository.getCurrentFile().nameWithoutExtension}.root.components.schemas.referencedSchema"
        assertTrue(
            modelRepositoryPaths.containsKey(expectedPathForReferencedSchema),
            "Path for referencedSchema should be registered"
        )
    }

    @Test
    fun `parsed schemas and properties retain their source locations`() {
        val file = TestResources.file("asyncapi_kafka_single_file_example.yaml")
        val document = DocumentReaderRegistry.read(file)
        val rootNode = ParserNodeFactory.root(document, context)
        parser.parse(rootNode)
        val schemaPath =
            "${context.sourceRepository.getCurrentFile().nameWithoutExtension}.root.components.schemas.simpleString"
        val simpleStringSchema = assertIs<Schema>(context.modelRepository.getModelsByPath()[schemaPath])

        val schemaLocation = assertNotNull(context.getSourceLocation(simpleStringSchema))
        assertEquals("asyncapi_kafka_single_file_example.yaml", schemaLocation.file.name)
        assertEquals("asyncapi_kafka_single_file_example.root.components.schemas.simpleString", schemaLocation.path)
        assertEquals(415, schemaLocation.line)
        assertTrue(schemaLocation.column > 0)

        val titleLocation = assertNotNull(
            context.getSourceLocation(simpleStringSchema, simpleStringSchema::title)
        )
        assertEquals("asyncapi_kafka_single_file_example.yaml", titleLocation.file.name)
        assertEquals("asyncapi_kafka_single_file_example.root.components.schemas.simpleString.title", titleLocation.path)
        assertEquals(416, titleLocation.line)
        assertTrue(titleLocation.column > 0)
        assertEquals(417, context.getLine(simpleStringSchema, simpleStringSchema::description))
        assertEquals(418, context.getLine(simpleStringSchema, simpleStringSchema::type))
    }

    @Test
    fun `parse document missing AsyncAPI version reports the required member and source`() {
        val file = TestResources.file("parser/asyncapi/asyncapi_parser_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val rootNode = ParserNodeFactory.root(document, context)
            .expectObject().required("cases")
            .expectObject().required("MissingVersion")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(rootNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("asyncapi", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals("asyncapi_parser_invalid.root.cases.MissingVersion.asyncapi", diagnostic.path)
        assertEquals("root.cases.MissingVersion", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse document with boolean AsyncAPI version reports its expected type and source`() {
        val file = TestResources.file("parser/asyncapi/asyncapi_parser_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val rootNode = ParserNodeFactory.root(document, context)
            .expectObject().required("cases")
            .expectObject().required("BooleanVersion")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(rootNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.BOOLEAN, diagnostic.actualType)
        assertEquals(false, diagnostic.actualValue)
        assertEquals("asyncapi_parser_invalid.root.cases.BooleanVersion.asyncapi", diagnostic.path)
        assertEquals("root.cases.BooleanVersion.asyncapi", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse document with malformed specification version reports its source`() {
        val file = TestResources.file("parser/asyncapi/asyncapi_parser_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val rootNode = ParserNodeFactory.root(document, context)
            .expectObject().required("cases")
            .expectObject().required("MalformedVersion")

        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(rootNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.InvalidSpecificationVersion>(exception.diagnostic)

        assertEquals(ParserDiagnosticCategory.INVALID_SPECIFICATION_VERSION, diagnostic.category)
        assertEquals("3.0", diagnostic.declaredVersion)
        assertEquals("asyncapi_parser_invalid.root.cases.MalformedVersion.asyncapi", diagnostic.path)
        assertEquals("asyncapi_parser_invalid.yaml", diagnostic.sourceLocation.file.name)
        assertEquals(12, diagnostic.sourceLocation.line)
        assertTrue(exception.message.orEmpty().contains("expected major.minor.patch"))
    }

    @Test
    fun `parse document distinguishes a known unimplemented specification version`() {
        val file = TestResources.file("parser/asyncapi/asyncapi_parser_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val rootNode = ParserNodeFactory.root(document, context)
            .expectObject().required("cases")
            .expectObject().required("KnownUnimplementedVersion")

        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(rootNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnsupportedSpecificationVersion>(exception.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNSUPPORTED_SPECIFICATION_VERSION, diagnostic.category)
        assertEquals("3.1.0", diagnostic.declaredVersion)
        assertTrue(diagnostic.knownVersionLine)
        assertEquals(listOf("3.0.x"), diagnostic.supportedVersionLines)
        assertTrue(exception.message.orEmpty().contains("recognized, but its parser profile is not implemented"))
    }

    @Test
    fun `parse document rejects an unknown future specification version`() {
        val file = TestResources.file("parser/asyncapi/asyncapi_parser_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val rootNode = ParserNodeFactory.root(document, context)
            .expectObject().required("cases")
            .expectObject().required("UnknownVersion")

        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(rootNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnsupportedSpecificationVersion>(exception.diagnostic)

        assertEquals("3.5.0", diagnostic.declaredVersion)
        assertEquals(false, diagnostic.knownVersionLine)
        assertTrue(exception.message.orEmpty().contains("is not supported"))
        assertTrue(exception.message.orEmpty().contains("Supported version lines: 3.0.x"))
    }

    @Test
    fun `parse document rejects an old specification major version`() {
        val file = TestResources.file("parser/asyncapi/asyncapi_parser_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val rootNode = ParserNodeFactory.root(document, context)
            .expectObject().required("cases")
            .expectObject().required("OldMajorVersion")

        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(rootNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnsupportedSpecificationVersion>(exception.diagnostic)

        assertEquals("2.6.0", diagnostic.declaredVersion)
        assertEquals(false, diagnostic.knownVersionLine)
    }

    @Test
    fun `supported patch and suffixed versions retain their declared value`() {
        val file = TestResources.file("parser/asyncapi/asyncapi_parser_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val cases = ParserNodeFactory.root(document, context)
            .expectObject().required("cases")
            .expectObject()

        val patchVersion = parser.parse(cases.required("SupportedPatchVersion"))
        val suffixedVersion = parser.parse(cases.required("SupportedSuffixedVersion"))

        assertEquals("3.0.7", patchVersion.asyncapi)
        assertEquals("3.0.0-rc1", suffixedVersion.asyncapi)
    }

    @Test
    fun `parse document missing info reports the required member and source`() {
        val file = TestResources.file("parser/asyncapi/asyncapi_parser_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val rootNode = ParserNodeFactory.root(document, context)
            .expectObject().required("cases")
            .expectObject().required("MissingInfo")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(rootNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("info", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals("asyncapi_parser_invalid.root.cases.MissingInfo.info", diagnostic.path)
        assertEquals("root.cases.MissingInfo", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse document with null default content type reports its expected type and source`() {
        val file = TestResources.file("parser/asyncapi/asyncapi_parser_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val rootNode = ParserNodeFactory.root(document, context)
            .expectObject().required("cases")
            .expectObject().required("NullDefaultContentType")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(rootNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertEquals(null, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_invalid.root.cases.NullDefaultContentType.defaultContentType",
            diagnostic.path,
        )
        assertEquals("root.cases.NullDefaultContentType.defaultContentType", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
