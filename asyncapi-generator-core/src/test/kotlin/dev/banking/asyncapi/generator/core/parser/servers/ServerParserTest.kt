package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ServerParserTest {

    private val context = AsyncApiContext()
    private val parser = ServerParser(context)

    @Test
    fun `parses inline and referenced servers with all optional fields`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_servers_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val serversNode = ParserNodeFactory.root(document, context)
            .expectObject().required("servers")

        val result = parser.parseMap(serversNode)

        val scram = assertIs<ServerInterface.ServerInline>(result["scram-connections"]).server
        assertEquals("test.mykafkacluster.org:{port}/{environment}", scram.host)
        assertEquals("kafka-secure", scram.protocol)
        assertEquals("3.7", scram.protocolVersion)
        assertEquals("/vhosts/{environment}", scram.pathname)
        assertEquals("SCRAM test broker", scram.title)
        assertEquals("Kafka broker secured with SCRAM", scram.summary)
        assertEquals("Test broker secured with scramSha256", scram.description)
        val security = assertIs<SecuritySchemeInterface.SecuritySchemeInline>(scram.security?.single()).security
        assertEquals("scramSha256", security.type)
        val binding = assertIs<BindingInterface.BindingInline>(scram.bindings?.get("kafka")).binding
        assertEquals(mapOf("schemaRegistryUrl" to "https://registry.example.com"), binding.content)
        val externalDocs = assertIs<ExternalDocInterface.ExternalDocInline>(scram.externalDocs).externalDoc
        assertEquals("https://example.com/docs/server", externalDocs.url)
        val scramTags = assertNotNull(scram.tags).map { assertIs<TagInterface.TagInline>(it).tag }
        assertEquals(listOf("env:test-scram", "kind:remote", "visibility:private"), scramTags.map { it.name })

        assertEquals(setOf("port", "environment"), scram.variables?.keys)

        val mtls = assertIs<ServerInterface.ServerInline>(result["mtls-connections"]).server
        assertEquals("test.mykafkacluster.org:28092", mtls.host)
        assertEquals("kafka-secure", mtls.protocol)
        assertEquals("Test broker secured with X509", mtls.description)
        assertEquals(3, mtls.tags?.size)

        val staging = assertIs<ServerInterface.ServerReference>(result["staging"]).reference
        assertEquals("#/components/servers/stagingServer", staging.ref)
        assertEquals(SERVER, staging.referenceCategoryKey)
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

}
