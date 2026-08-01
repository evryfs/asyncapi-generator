package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ServerParserTest {

    private val context = AsyncApiContext()
    private val parser = ServerParser(context)

    @Test
    fun `parse inline and referenced servers`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_servers_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val serversNode = ParserNodeFactory.root(document, context)
            .expectObject().required("servers")

        val result = parser.parseMap(serversNode)

        val scram = assertIs<ServerInterface.ServerInline>(result["scram-connections"]).server
        assertEquals("test.mykafkacluster.org:{port}/{environment}", scram.host)
        assertEquals("kafka-secure", scram.protocol)
        assertEquals("Test broker secured with scramSha256", scram.description)
        val scramTags = assertNotNull(scram.tags).map { assertIs<TagInterface.TagInline>(it).tag }
        assertEquals(listOf("env:test-scram", "kind:remote", "visibility:private"), scramTags.map { it.name })

        val port = assertIs<ServerVariableInterface.ServerVariableInline>(scram.variables?.get("port"))
            .serverVariable
        assertEquals(listOf("18092", "28092"), port.enum)
        assertEquals("18092", port.default)
        assertEquals("The port used for Kafka connections", port.description)
        val environment =
            assertIs<ServerVariableInterface.ServerVariableInline>(scram.variables?.get("environment"))
                .serverVariable
        assertEquals(listOf("test", "staging", "prod"), environment.enum)
        assertEquals("test", environment.default)
        assertEquals("Deployment environment", environment.description)

        val mtls = assertIs<ServerInterface.ServerInline>(result["mtls-connections"]).server
        assertEquals("test.mykafkacluster.org:28092", mtls.host)
        assertEquals("kafka-secure", mtls.protocol)
        assertEquals("Test broker secured with X509", mtls.description)
        val mtlsTags = assertNotNull(mtls.tags).map { assertIs<TagInterface.TagInline>(it).tag }
        assertEquals(listOf("env:test-mtls", "kind:remote", "visibility:private"), mtlsTags.map { it.name })

        val staging = assertIs<ServerInterface.ServerReference>(result["staging"]).reference
        assertEquals("#/components/servers/stagingServer", staging.ref)
        assertEquals(SERVER, staging.referenceCategoryKey)
    }

    @Test
    fun `parse server with invalid variables structure reports its expected type and source`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_server_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val serversNode = ParserNodeFactory.root(document, context)
            .expectObject().required("servers")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(serversNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-a-map", diagnostic.actualValue)
        assertEquals("asyncapi_parser_server_invalid.root.servers.InvalidVariables.variables", diagnostic.path)
        assertEquals("root.servers.InvalidVariables.variables", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_server_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse server with null reference reports the reference type and source`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_server_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val serversNode = ParserNodeFactory.root(document, context)
            .expectObject().required("serverCases")
            .expectObject().required("NullReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(serversNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_server_invalid.root.serverCases.NullReference.invalidReference.\$ref",
            diagnostic.path,
        )
        assertEquals("root.serverCases.NullReference.invalidReference.\$ref", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_server_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse server missing host reports the required member and source`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_server_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val serversNode = ParserNodeFactory.root(document, context)
            .expectObject().required("serverCases")
            .expectObject().required("MissingHost")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(serversNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("host", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals("asyncapi_parser_server_invalid.root.serverCases.MissingHost.missingHost.host", diagnostic.path)
        assertEquals("root.serverCases.MissingHost.missingHost", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_server_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse server map from an array reports the container type and source`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_server_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val serversNode = ParserNodeFactory.root(document, context)
            .expectObject().required("serverCases")
            .expectObject().required("ArrayInsteadOfMap")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(serversNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.ARRAY, diagnostic.actualType)
        assertEquals(listOf(mapOf("host" to "localhost", "protocol" to "kafka")), diagnostic.actualValue)
        assertEquals("asyncapi_parser_server_invalid.root.serverCases.ArrayInsteadOfMap", diagnostic.path)
        assertEquals("root.serverCases.ArrayInsteadOfMap", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_server_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
