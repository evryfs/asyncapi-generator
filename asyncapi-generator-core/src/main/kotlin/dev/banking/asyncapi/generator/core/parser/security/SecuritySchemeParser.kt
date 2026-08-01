package dev.banking.asyncapi.generator.core.parser.security

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.security.OAuthFlow
import dev.banking.asyncapi.generator.core.model.security.OAuthFlows
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SECURITY_SCHEME

/**
 * Parses AsyncAPI security scheme objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `SecuritySchemeParserTest`
 */
class SecuritySchemeParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, SecuritySchemeInterface> = buildMap {
        parserNode.members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseList(parserNode: ParserNode): List<SecuritySchemeInterface> = buildList {
        parserNode.elements().forEach { node ->
            add(parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): SecuritySchemeInterface {
        parserNode.optional($$"$ref")?.expect<String>()?.let { reference ->
            return SecuritySchemeInterface.SecuritySchemeReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = SECURITY_SCHEME
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        val type = parserNode.required("type").expect<String>()
        val description = parserNode.optional("description")?.expect<String>()
        val nameField = parserNode.optional("name")?.expect<String>()
        val inField = parserNode.optional("in")?.expect<String>()
        val scheme = parserNode.optional("scheme")?.expect<String>()
        val bearerFormat = parserNode.optional("bearerFormat")?.expect<String>()
        val openIdConnectUrl = parserNode.optional("openIdConnectUrl")?.expect<String>()
        val flows = parserNode.optional("flows")?.let(::parseFlows)
        val scopes = parserNode.optional("scopes")?.expect<List<String>>()
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
        return OAuthFlows(
            implicit = parserNode.optional("implicit")?.let(::parseFlow),
            password = parserNode.optional("password")?.let(::parseFlow),
            clientCredentials = parserNode.optional("clientCredentials")?.let(::parseFlow),
            authorizationCode = parserNode.optional("authorizationCode")?.let(::parseFlow),
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun parseFlow(parserNode: ParserNode): OAuthFlow {
        val authorizationUrl = parserNode.optional("authorizationUrl")?.expect<String>()
        val tokenUrl = parserNode.optional("tokenUrl")?.expect<String>()
        val refreshUrl = parserNode.optional("refreshUrl")?.expect<String>()
        val availableScopes = parserNode.optional("availableScopes")?.expect<Map<String, String>>()
        return OAuthFlow(
            authorizationUrl = authorizationUrl,
            tokenUrl = tokenUrl,
            refreshUrl = refreshUrl,
            availableScopes = availableScopes
        ).also { asyncApiContext.register(it, parserNode) }
    }
}
