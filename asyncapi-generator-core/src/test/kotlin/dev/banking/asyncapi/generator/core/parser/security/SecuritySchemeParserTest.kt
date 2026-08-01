package dev.banking.asyncapi.generator.core.parser.security

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecuritySchemeParserTest : ParserTestSupport() {

    private val parser = SecuritySchemeParser(asyncApiContext)

    @Test
    fun parseSecuritySchemes_validate_data_classes_saslScram_certs_basicAuth() {
        val securitySchemesNode = readNode(
            "parser/security/asyncapi_parser_security_valid.yaml",
            "components",
            "securitySchemes",
        )
        val result = parser.parseMap(securitySchemesNode)

        assertTrue("saslScram" in result)
        assertTrue("certs" in result)
        assertTrue("basicAuth" in result)

        val saslScram = (result["saslScram"] as SecuritySchemeInterface.SecuritySchemeInline).security
        val expectedSaslScram = saslScram()
        assertThat(saslScram)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSaslScram)

        val certs = (result["certs"] as SecuritySchemeInterface.SecuritySchemeInline).security
        val expectedCerts = certs()
        assertThat(certs)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedCerts)

        val basicAuth = (result["basicAuth"] as SecuritySchemeInterface.SecuritySchemeInline).security
        val expectedBasicAuth = basicAuth()
        assertThat(basicAuth)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedBasicAuth)
    }

    @Test
    fun parseSecuritySchemes_validate_data_classes_bearerAuth_apiKeyHeader_apiKeyQuery() {
        val securitySchemesNode = readNode(
            "parser/security/asyncapi_parser_security_valid.yaml",
            "components",
            "securitySchemes",
        )
        val result = parser.parseMap(securitySchemesNode)

        assertTrue("bearerAuth" in result)
        assertTrue("apiKeyHeader" in result)
        assertTrue("apiKeyQuery" in result)

        val bearerAuth = (result["bearerAuth"] as SecuritySchemeInterface.SecuritySchemeInline).security
        val expectedBearerAuth = bearerAuth()
        assertThat(bearerAuth)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedBearerAuth)

        val apiKeyHeader = (result["apiKeyHeader"] as SecuritySchemeInterface.SecuritySchemeInline).security
        val expectedApiKeyHeader = apiKeyHeader()
        assertThat(apiKeyHeader)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedApiKeyHeader)

        val apiKeyQuery = (result["apiKeyQuery"] as SecuritySchemeInterface.SecuritySchemeInline).security
        val expectedApiKeyQuery = apiKeyQuery()
        assertThat(apiKeyQuery)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedApiKeyQuery)
    }

    @Test
    fun parseSecuritySchemes_validate_data_classes_openIdConnectExample_oauthExample() {
        val securitySchemesNode = readNode(
            "parser/security/asyncapi_parser_security_valid.yaml",
            "components",
            "securitySchemes",
        )
        val result = parser.parseMap(securitySchemesNode)

        assertTrue("openIdConnectExample" in result)
        assertTrue("oauthExample" in result)

        val openIdConnectExample =
            (result["openIdConnectExample"] as SecuritySchemeInterface.SecuritySchemeInline).security
        val expectedOpenIdConnectExample = openIdConnectExample()
        assertThat(openIdConnectExample)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedOpenIdConnectExample)

        val oauthExample = (result["oauthExample"] as SecuritySchemeInterface.SecuritySchemeInline).security
        val expectedOauthExample = oauthExample()
        assertThat(oauthExample)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedOauthExample)
    }

    @Test
    fun `parse security scheme missing type reports the required member and source`() {
        val schemeNode = readNode(
            "parser/security/asyncapi_parser_security_invalid.yaml",
            "components",
            "securitySchemes",
            "MissingType",
        )
        assertMissingRequiredMember(
            memberName = "type",
            path = "asyncapi_parser_security_invalid.root.components.securitySchemes.MissingType.type",
            sourcePath = "root.components.securitySchemes.MissingType",
            sourceFile = "asyncapi_parser_security_invalid.yaml",
        ) {
            parser.parseElement(schemeNode)
        }
    }

    @Test
    fun `parse security scheme with invalid flows structure reports its expected type and source`() {
        val schemeNode = readNode(
            "parser/security/asyncapi_parser_security_invalid.yaml",
            "components",
            "securitySchemes",
            "InvalidFlowsStructure",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "not-an-object",
            path = "asyncapi_parser_security_invalid.root.components.securitySchemes.InvalidFlowsStructure.flows",
            sourcePath = "root.components.securitySchemes.InvalidFlowsStructure.flows",
            sourceFile = "asyncapi_parser_security_invalid.yaml",
        ) {
            parser.parseElement(schemeNode)
        }
    }

    @Test
    fun `parse security scheme with null reference reports its expected type and source`() {
        val schemeNode = readNode(
            "parser/security/asyncapi_parser_security_invalid.yaml",
            "components",
            "securitySchemes",
            "NullReference",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_security_invalid.root.components.securitySchemes.NullReference.\$ref",
            sourcePath = "root.components.securitySchemes.NullReference.\$ref",
            sourceFile = "asyncapi_parser_security_invalid.yaml",
        ) {
            parser.parseElement(schemeNode)
        }
    }

    @Test
    fun `parse OAuth flow with numeric scope description reports the nested value and source`() {
        val schemeNode = readNode(
            "parser/security/asyncapi_parser_security_invalid.yaml",
            "components",
            "securitySchemes",
            "InvalidAvailableScope",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NUMBER,
            actualValue = 7,
            path = "asyncapi_parser_security_invalid.root.components.securitySchemes.InvalidAvailableScope.flows.implicit.availableScopes.invalid",
            sourcePath = "root.components.securitySchemes.InvalidAvailableScope.flows.implicit.availableScopes.invalid",
            sourceFile = "asyncapi_parser_security_invalid.yaml",
        ) {
            parser.parseElement(schemeNode)
        }
    }

    @Test
    fun `parse security list from an object reports the container type and source`() {
        val schemesNode = readNode(
            "parser/security/asyncapi_parser_security_invalid.yaml",
            "components",
            "securitySchemeCases",
            "ObjectInsteadOfList",
        )
        assertUnexpectedValueType(
            expectedType = "List<Any?>",
            actualType = ParserValueType.OBJECT,
            actualValue = mapOf("invalidScheme" to mapOf("type" to "userPassword")),
            path = "asyncapi_parser_security_invalid.root.components.securitySchemeCases.ObjectInsteadOfList",
            sourcePath = "root.components.securitySchemeCases.ObjectInsteadOfList",
            sourceFile = "asyncapi_parser_security_invalid.yaml",
        ) {
            parser.parseList(schemesNode)
        }
    }
}
