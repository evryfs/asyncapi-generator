package dev.banking.asyncapi.generator.core.parser.asyncapi

import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AsyncApiParserTest : ParserTestSupport() {

    private val parser = AsyncApiParser(asyncApiContext)

    @Test
    fun parseSingleFileAsyncApi() {
        val rootNode = readNode("asyncapi_kafka_single_file_example.yaml")
        val result = parser.parse(rootNode)

        assertEquals("3.0.0", result.asyncapi, "AsyncAPI version mismatch")
        assertEquals("Streetlights Kafka API", result.info.title)
        assertEquals("1.0.0", result.info.version)
        assertEquals("application/json", result.defaultContentType)

        assertNotNull(result.info, "Missing info object")
        assertNotNull(result.servers, "Missing servers object")
        assertNotNull(result.channels, "Missing channels object")
        assertNotNull(result.operations, "Missing operations object")
        assertNotNull(result.components, "Missing components object")

        val serverCount = result.servers.size
        val channelCount = result.channels.size
        val operationCount = result.operations.size

        assertEquals(3, serverCount, "Expected 3 servers (scram, mtls, staging)")
        assertEquals(8, channelCount, "Expected 8 channels total")
        assertEquals(4, operationCount, "Expected 4 operations total")

        val components = (result.components as? ComponentInterface.ComponentInline)?.component
        val schemaCount = components?.schemas?.size
        val messageCount = components?.messages?.size
        val securitySchemeCount = components?.securitySchemes?.size
        val parameterCount = components?.parameters?.size
        val operationTraitCount = components?.operationTraits?.size
        val messageTraitCount = components?.messageTraits?.size

        assertEquals(15, schemaCount, "Expected 15 schemas under components")
        assertEquals(4, messageCount, "Expected 4 messages under components")
        assertEquals(8, securitySchemeCount, "Expected 8 security schemes under components")
        assertEquals(2, parameterCount, "Expected 2 parameters under components")
        assertEquals(2, operationTraitCount, "Expected 2 operation traits (logging, kafka)")
        assertEquals(1, messageTraitCount, "Expected 1 message trait (commonHeaders)")
    }

    @Test
    fun `parses equivalent yaml and json documents into the same model`() {
        val yaml = parseDocument("parser/asyncapi/format-independent.yaml")
        val json = parseDocument("parser/asyncapi/format-independent.json")

        assertEquals(yaml, json)
    }

    @Test
    fun `parsed schema is registered in model repository`() {
        val rootNode = readNode("asyncapi_kafka_single_file_example.yaml")
        val asyncApi = parser.parse(rootNode)
        val schemasMap = (asyncApi.components as ComponentInterface.ComponentInline).component.schemas!!

        val lightMeasuredPayloadSchema = (schemasMap["lightMeasuredPayload"] as SchemaInterface.SchemaInline).schema
        val referencedSchema = (schemasMap["referencedSchema"] as SchemaInterface.SchemaReference).reference
        val modelRepositoryModels = asyncApiContext.modelRepository.getModelsByInstance()

        assertNotNull(
            modelRepositoryModels[lightMeasuredPayloadSchema],
            "lightMeasuredPayload Schema should be registered"
        )
        assertNotNull(
            modelRepositoryModels[referencedSchema],
            "referencedSchema Reference should be registered"
        )
        val nestedLumensSchema =
            ((lightMeasuredPayloadSchema.properties!!["lumens"] as SchemaInterface.SchemaInline).schema)
        assertNotNull(
            modelRepositoryModels[nestedLumensSchema],
            "Nested lumens schema should be registered"
        )
        val modelRepositoryPaths = asyncApiContext.modelRepository.getModelsByPath()
        val expectedPathForLightMeasuredPayload =
            "${asyncApiContext.sourceRepository.getCurrentFile().nameWithoutExtension}.root.components.schemas.lightMeasuredPayload"
        assertTrue(
            modelRepositoryPaths.containsKey(expectedPathForLightMeasuredPayload),
            "Path for lightMeasuredPayload should be registered"
        )
        val expectedPathForReferencedSchema =
            "${asyncApiContext.sourceRepository.getCurrentFile().nameWithoutExtension}.root.components.schemas.referencedSchema"
        assertTrue(
            modelRepositoryPaths.containsKey(expectedPathForReferencedSchema),
            "Path for referencedSchema should be registered"
        )
    }

    @Test
    fun `parsed schema properties have correct line numbers in source repository`() {
        val rootNode = readNode("asyncapi_kafka_single_file_example.yaml")
        parser.parse(rootNode)
        val schemaPath =
            "${asyncApiContext.sourceRepository.getCurrentFile().nameWithoutExtension}.root.components.schemas.simpleString"
        val simpleStringSchema = asyncApiContext.modelRepository.getModelsByPath()[schemaPath] as Schema
        assertNotNull(
            simpleStringSchema,
            "simpleString schema should be retrievable by path from ModelRepository"
        )
        assertEquals(431, asyncApiContext.getLine(simpleStringSchema, simpleStringSchema::title))
        assertEquals(432, asyncApiContext.getLine(simpleStringSchema, simpleStringSchema::description))
        assertEquals(433, asyncApiContext.getLine(simpleStringSchema, simpleStringSchema::type))
    }

    @Test
    fun `parsed schema properties preserve source location links`() {
        val rootNode = readNode("asyncapi_kafka_single_file_example.yaml")
        parser.parse(rootNode)
        val schemaPath =
            "${asyncApiContext.sourceRepository.getCurrentFile().nameWithoutExtension}.root.components.schemas.simpleString"
        val simpleStringSchema = asyncApiContext.modelRepository.getModelsByPath()[schemaPath] as Schema

        val schemaLocation = assertNotNull(asyncApiContext.getSourceLocation(simpleStringSchema))
        assertEquals("asyncapi_kafka_single_file_example.yaml", schemaLocation.file.name)
        assertEquals("asyncapi_kafka_single_file_example.root.components.schemas.simpleString", schemaLocation.path)
        assertEquals(430, schemaLocation.line)
        assertTrue(schemaLocation.column > 0)

        val titleLocation = assertNotNull(
            asyncApiContext.getSourceLocation(simpleStringSchema, simpleStringSchema::title)
        )
        assertEquals("asyncapi_kafka_single_file_example.yaml", titleLocation.file.name)
        assertEquals("asyncapi_kafka_single_file_example.root.components.schemas.simpleString.title", titleLocation.path)
        assertEquals(431, titleLocation.line)
        assertTrue(titleLocation.column > 0)
    }

    @Test
    fun `parse document missing AsyncAPI version reports the required member and source`() {
        val rootNode = invalidCase("MissingVersion")
        assertMissingRequiredMember(
            memberName = "asyncapi",
            path = "asyncapi_parser_invalid.root.cases.MissingVersion.asyncapi",
            sourcePath = "root.cases.MissingVersion",
            sourceFile = "asyncapi_parser_invalid.yaml",
        ) {
            parser.parse(rootNode)
        }
    }

    @Test
    fun `parse document with boolean AsyncAPI version reports its expected type and source`() {
        val rootNode = invalidCase("BooleanVersion")
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.BOOLEAN,
            actualValue = false,
            path = "asyncapi_parser_invalid.root.cases.BooleanVersion.asyncapi",
            sourcePath = "root.cases.BooleanVersion.asyncapi",
            sourceFile = "asyncapi_parser_invalid.yaml",
        ) {
            parser.parse(rootNode)
        }
    }

    @Test
    fun `parse document with malformed specification version reports its source`() {
        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(invalidCase("MalformedVersion"))
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
        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(invalidCase("KnownUnimplementedVersion"))
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
        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(invalidCase("UnknownVersion"))
        }
        val diagnostic = assertIs<ParserDiagnostic.UnsupportedSpecificationVersion>(exception.diagnostic)

        assertEquals("3.5.0", diagnostic.declaredVersion)
        assertEquals(false, diagnostic.knownVersionLine)
        assertTrue(exception.message.orEmpty().contains("is not supported"))
        assertTrue(exception.message.orEmpty().contains("Supported version lines: 3.0.x"))
    }

    @Test
    fun `parse document rejects an old specification major version`() {
        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parse(invalidCase("OldMajorVersion"))
        }
        val diagnostic = assertIs<ParserDiagnostic.UnsupportedSpecificationVersion>(exception.diagnostic)

        assertEquals("2.6.0", diagnostic.declaredVersion)
        assertEquals(false, diagnostic.knownVersionLine)
    }

    @Test
    fun `supported patch and suffixed versions retain their declared value`() {
        assertEquals("3.0.7", parser.parse(invalidCase("SupportedPatchVersion")).asyncapi)
        assertEquals("3.0.0-rc1", parser.parse(invalidCase("SupportedSuffixedVersion")).asyncapi)
    }

    @Test
    fun `parse document missing info reports the required member and source`() {
        val rootNode = invalidCase("MissingInfo")
        assertMissingRequiredMember(
            memberName = "info",
            path = "asyncapi_parser_invalid.root.cases.MissingInfo.info",
            sourcePath = "root.cases.MissingInfo",
            sourceFile = "asyncapi_parser_invalid.yaml",
        ) {
            parser.parse(rootNode)
        }
    }

    @Test
    fun `parse document with null default content type reports its expected type and source`() {
        val rootNode = invalidCase("NullDefaultContentType")
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_invalid.root.cases.NullDefaultContentType.defaultContentType",
            sourcePath = "root.cases.NullDefaultContentType.defaultContentType",
            sourceFile = "asyncapi_parser_invalid.yaml",
        ) {
            parser.parse(rootNode)
        }
    }

    private fun invalidCase(name: String) =
        readNode(
            "parser/asyncapi/asyncapi_parser_invalid.yaml",
            "cases",
            name,
        )
}
