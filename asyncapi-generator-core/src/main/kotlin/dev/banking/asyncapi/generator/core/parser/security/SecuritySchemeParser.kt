package dev.banking.asyncapi.generator.core.parser.security

import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SECURITY_SCHEME
import dev.banking.asyncapi.generator.core.model.security.OAuthFlow
import dev.banking.asyncapi.generator.core.model.security.OAuthFlows
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseObject
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.OAUTH_FLOW
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.OAUTH_FLOWS
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.SECURITY_SCHEME as SECURITY_SCHEME_OBJECT

/**
 * Parses AsyncAPI security scheme objects from parser nodes.
 */
internal class SecuritySchemeParser {
    fun parseElement(parserNode: ParserNode): SecuritySchemeInterface =
        parserNode.parseReferenceable(
            category = SECURITY_SCHEME,
            objectType = SECURITY_SCHEME_OBJECT,
            reference = SecuritySchemeInterface::SecuritySchemeReference,
            inline = SecuritySchemeInterface::SecuritySchemeInline,
        ) { objectNode ->
            SecurityScheme(
                type = objectNode.required("type").expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                name = objectNode.optional("name")?.expect<String>(),
                inField = objectNode.optional("in")?.expect<String>(),
                scheme = objectNode.optional("scheme")?.expect<String>(),
                bearerFormat = objectNode.optional("bearerFormat")?.expect<String>(),
                openIdConnectUrl = objectNode.optional("openIdConnectUrl")?.expect<String>(),
                flows = objectNode.optional("flows")?.let(::parseFlows),
                scopes = objectNode.optional("scopes")?.expect<List<String>>(),
                extensions = objectNode.extensions(),
            )
        }

    private fun parseFlows(parserNode: ParserNode): OAuthFlows =
        parserNode.parseObject(OAUTH_FLOWS) { objectNode ->
            OAuthFlows(
                implicit = objectNode.optional("implicit")?.let(::parseFlow),
                password = objectNode.optional("password")?.let(::parseFlow),
                clientCredentials = objectNode.optional("clientCredentials")?.let(::parseFlow),
                authorizationCode = objectNode.optional("authorizationCode")?.let(::parseFlow),
            )
        }

    private fun parseFlow(parserNode: ParserNode): OAuthFlow =
        parserNode.parseObject(OAUTH_FLOW) { objectNode ->
            OAuthFlow(
                authorizationUrl = objectNode.optional("authorizationUrl")?.expect<String>(),
                tokenUrl = objectNode.optional("tokenUrl")?.expect<String>(),
                refreshUrl = objectNode.optional("refreshUrl")?.expect<String>(),
                availableScopes = objectNode.required("availableScopes").expect<Map<String, String>>(),
            )
        }
}
