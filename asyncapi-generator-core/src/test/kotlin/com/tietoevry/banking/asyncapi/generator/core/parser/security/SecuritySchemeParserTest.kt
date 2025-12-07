package com.tietoevry.banking.asyncapi.generator.core.parser.security

import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import com.tietoevry.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class SecuritySchemeParserTest : AbstractParserTest() {

    private val parser = SecuritySchemeParser(asyncApiContext)

    @Test
    fun parseSecuritySchemes_validate_data_classes_saslScram_certs_basicAuth() {
        val root = readYaml("src/test/resources/parser/security/asyncapi_parser_security_valid.yaml")
        val result = parser.parseMap(root.mandatory("components").mandatory("securitySchemes"))

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
        val root = readYaml("src/test/resources/parser/security/asyncapi_parser_security_valid.yaml")
        val result = parser.parseMap(root.mandatory("components").mandatory("securitySchemes"))

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
        val root = readYaml("src/test/resources/parser/security/asyncapi_parser_security_valid.yaml")
        val result = parser.parseMap(root.mandatory("components").mandatory("securitySchemes"))

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
    fun `parse security scheme missing type throws RequiredObject`() {
        val root = readYaml("src/test/resources/parser/security/asyncapi_parser_security_invalid.yaml")
        val schemeNode = root
            .mandatory("components")
            .mandatory("securitySchemes")
            .mandatory("MissingType")
        assertFailsWith<AsyncApiParseException.Mandatory> {
            parser.parseElement(schemeNode)
        }
    }

    @Test
    fun `parse security scheme with invalid flows structure throws UnexpectedValue`() {
        val root = readYaml("src/test/resources/parser/security/asyncapi_parser_security_invalid.yaml")
        val schemeNode = root
            .mandatory("components")
            .mandatory("securitySchemes")
            .mandatory("InvalidFlowsStructure")
        assertFailsWith<AsyncApiParseException.UnexpectedValue> {
            parser.parseElement(schemeNode)
        }
    }
}
