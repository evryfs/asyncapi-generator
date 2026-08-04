package dev.banking.asyncapi.generator.core.parser.security

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.security.OAuthFlow
import dev.banking.asyncapi.generator.core.model.security.OAuthFlows
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SECURITY_SCHEME
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.OAUTH_FLOW
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.OAUTH_FLOWS
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.SECURITY_SCHEME as SECURITY_SCHEME_OBJECT

/**
 * Parses AsyncAPI security scheme objects from parser nodes.
 */
internal class SecuritySchemeParser(
    private val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, SecuritySchemeInterface> =
        parserNode.expectObject().members().associate { node ->
            node.name to parseElement(node)
        }

    fun parseList(parserNode: ParserNode): List<SecuritySchemeInterface> =
        parserNode.expectArray().elements().map(::parseElement)

    fun parseElement(parserNode: ParserNode): SecuritySchemeInterface {
        val objectNode = parserNode.expectObject()
        objectNode.optional($$"$ref")?.expect<String>()?.let { reference ->
            return SecuritySchemeInterface.SecuritySchemeReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = SECURITY_SCHEME
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        objectNode.expectOnlyMembers(SECURITY_SCHEME_OBJECT)
        val type = objectNode.required("type").expect<String>()
        val description = objectNode.optional("description")?.expect<String>()
        val nameField = objectNode.optional("name")?.expect<String>()
        val inField = objectNode.optional("in")?.expect<String>()
        val scheme = objectNode.optional("scheme")?.expect<String>()
        val bearerFormat = objectNode.optional("bearerFormat")?.expect<String>()
        val openIdConnectUrl = objectNode.optional("openIdConnectUrl")?.expect<String>()
        val flows = objectNode.optional("flows")?.let(::parseFlows)
        val scopes = objectNode.optional("scopes")?.expect<List<String>>()
        return SecuritySchemeInterface.SecuritySchemeInline(
            SecurityScheme(
                type = type,
                description = description,
                name = nameField,
                inField = inField,
                scheme = scheme,
                bearerFormat = bearerFormat,
                openIdConnectUrl = openIdConnectUrl,
                flows = flows,
                scopes = scopes
            ).also { asyncApiContext.register(it, parserNode) }
        )
    }

    private fun parseFlows(parserNode: ParserNode): OAuthFlows {
        val objectNode = parserNode.expectObject()
        objectNode.expectOnlyMembers(OAUTH_FLOWS)
        return OAuthFlows(
            implicit = objectNode.optional("implicit")?.let(::parseFlow),
            password = objectNode.optional("password")?.let(::parseFlow),
            clientCredentials = objectNode.optional("clientCredentials")?.let(::parseFlow),
            authorizationCode = objectNode.optional("authorizationCode")?.let(::parseFlow),
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun parseFlow(parserNode: ParserNode): OAuthFlow {
        val objectNode = parserNode.expectObject()
        objectNode.expectOnlyMembers(OAUTH_FLOW)
        val authorizationUrl = objectNode.optional("authorizationUrl")?.expect<String>()
        val tokenUrl = objectNode.optional("tokenUrl")?.expect<String>()
        val refreshUrl = objectNode.optional("refreshUrl")?.expect<String>()
        val availableScopes = objectNode.required("availableScopes").expect<Map<String, String>>()
        return OAuthFlow(
            authorizationUrl = authorizationUrl,
            tokenUrl = tokenUrl,
            refreshUrl = refreshUrl,
            availableScopes = availableScopes
        ).also { asyncApiContext.register(it, parserNode) }
    }
}
