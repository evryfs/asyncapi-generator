package dev.banking.asyncapi.generator.core.parser.security

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SECURITY_SCHEME
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SecuritySchemeParserTest {

    private val context = AsyncApiContext()
    private val parser = SecuritySchemeParser(context)

    @Test
    fun `parse SCRAM certificate and basic authentication schemes`() {
        val file = TestResources.file("parser/security/asyncapi_parser_security_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val securitySchemesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("securitySchemes")

        val result = parser.parseMap(securitySchemesNode)

        val saslScram = assertIs<SecuritySchemeInterface.SecuritySchemeInline>(result["saslScram"]).security
        assertEquals("scramSha256", saslScram.type)
        assertEquals("Provide your username and password for SASL/SCRAM authentication", saslScram.description)

        val certs = assertIs<SecuritySchemeInterface.SecuritySchemeInline>(result["certs"]).security
        assertEquals("X509", certs.type)
        assertEquals("Download the certificate files from the service provider", certs.description)

        val basicAuth = assertIs<SecuritySchemeInterface.SecuritySchemeInline>(result["basicAuth"]).security
        assertEquals("http", basicAuth.type)
        assertEquals("Basic HTTP authentication using username and password", basicAuth.description)
        assertEquals("basic", basicAuth.scheme)

        val reference =
            assertIs<SecuritySchemeInterface.SecuritySchemeReference>(result["referencedBasicAuth"]).reference
        assertEquals("#/components/securitySchemes/basicAuth", reference.ref)
        assertEquals(SECURITY_SCHEME, reference.referenceCategoryKey)
    }

    @Test
    fun `parse bearer and API key authentication schemes`() {
        val file = TestResources.file("parser/security/asyncapi_parser_security_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val securitySchemesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("securitySchemes")

        val result = parser.parseMap(securitySchemesNode)

        val bearerAuth = assertIs<SecuritySchemeInterface.SecuritySchemeInline>(result["bearerAuth"]).security
        assertEquals("http", bearerAuth.type)
        assertEquals("Bearer token authentication with JWT format", bearerAuth.description)
        assertEquals("bearer", bearerAuth.scheme)
        assertEquals("JWT", bearerAuth.bearerFormat)

        val apiKeyHeader = assertIs<SecuritySchemeInterface.SecuritySchemeInline>(result["apiKeyHeader"]).security
        assertEquals("httpApiKey", apiKeyHeader.type)
        assertEquals("API key passed in HTTP header", apiKeyHeader.description)
        assertEquals("X-API-Key", apiKeyHeader.name)
        assertEquals("header", apiKeyHeader.inField)

        val apiKeyQuery = assertIs<SecuritySchemeInterface.SecuritySchemeInline>(result["apiKeyQuery"]).security
        assertEquals("httpApiKey", apiKeyQuery.type)
        assertEquals("API key passed in query parameter", apiKeyQuery.description)
        assertEquals("apiKey", apiKeyQuery.name)
        assertEquals("query", apiKeyQuery.inField)
    }

    @Test
    fun `parse OpenID Connect and OAuth2 authentication schemes`() {
        val file = TestResources.file("parser/security/asyncapi_parser_security_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val securitySchemesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("securitySchemes")

        val result = parser.parseMap(securitySchemesNode)

        val openIdConnect =
            assertIs<SecuritySchemeInterface.SecuritySchemeInline>(result["openIdConnectExample"]).security
        assertEquals("openIdConnect", openIdConnect.type)
        assertEquals("OpenID Connect discovery URL example", openIdConnect.description)
        assertEquals("https://example.com/.well-known/openid-configuration", openIdConnect.openIdConnectUrl)

        val oauth = assertIs<SecuritySchemeInterface.SecuritySchemeInline>(result["oauthExample"]).security
        assertEquals("oauth2", oauth.type)
        assertEquals("Example OAuth2 flow", oauth.description)
        assertEquals(listOf("read:pets", "write:pets"), oauth.scopes)

        val flows = assertNotNull(oauth.flows)
        assertEquals("https://example.com/api/oauth/authorize", flows.implicit?.authorizationUrl)
        assertEquals(
            mapOf(
                "write:pets" to "modify pets in your account",
                "read:pets" to "read your pets",
            ),
            flows.implicit?.availableScopes,
        )
        assertEquals("https://example.com/api/oauth/token", flows.password?.tokenUrl)
        assertEquals(mapOf("admin" to "full access"), flows.password?.availableScopes)
        assertEquals("https://example.com/api/oauth/token", flows.clientCredentials?.tokenUrl)
        assertEquals(mapOf("write:docs" to "modify documents"), flows.clientCredentials?.availableScopes)
        assertEquals("https://example.com/api/oauth/authorize", flows.authorizationCode?.authorizationUrl)
        assertEquals("https://example.com/api/oauth/token", flows.authorizationCode?.tokenUrl)
        assertEquals(
            mapOf(
                "read:docs" to "read documents",
                "write:docs" to "modify documents",
            ),
            flows.authorizationCode?.availableScopes,
        )
    }

    @Test
    fun `parse security scheme missing type reports the required member and source`() {
        val file = TestResources.file("parser/security/asyncapi_parser_security_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemeNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("securitySchemes")
            .expectObject().required("MissingType")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemeNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("type", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals("asyncapi_parser_security_invalid.root.components.securitySchemes.MissingType.type", diagnostic.path)
        assertEquals("root.components.securitySchemes.MissingType", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_security_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse security scheme with invalid flows structure reports its expected type and source`() {
        val file = TestResources.file("parser/security/asyncapi_parser_security_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemeNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("securitySchemes")
            .expectObject().required("InvalidFlowsStructure")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemeNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-an-object", diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_security_invalid.root.components.securitySchemes.InvalidFlowsStructure.flows",
            diagnostic.path,
        )
        assertEquals("root.components.securitySchemes.InvalidFlowsStructure.flows", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_security_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse OAuth flow with numeric scope description reports the nested value and source`() {
        val file = TestResources.file("parser/security/asyncapi_parser_security_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemeNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("securitySchemes")
            .expectObject().required("InvalidAvailableScope")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemeNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(7, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_security_invalid.root.components.securitySchemes.InvalidAvailableScope.flows.implicit.availableScopes.invalid",
            diagnostic.path,
        )
        assertEquals(
            "root.components.securitySchemes.InvalidAvailableScope.flows.implicit.availableScopes.invalid",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_security_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse OAuth flow missing available scopes reports the required member and source`() {
        val file = TestResources.file("parser/security/asyncapi_parser_security_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemeNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("securitySchemes")
            .expectObject().required("MissingAvailableScopes")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemeNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("availableScopes", diagnostic.memberName)
        assertEquals(
            "asyncapi_parser_security_invalid.root.components.securitySchemes.MissingAvailableScopes." +
                "flows.authorizationCode.availableScopes",
            diagnostic.path,
        )
        assertEquals(
            "root.components.securitySchemes.MissingAvailableScopes.flows.authorizationCode",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_security_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

}
