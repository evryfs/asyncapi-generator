package com.tietoevry.banking.asyncapi.generator.core.parser.security

import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.security.OAuthFlow
import com.tietoevry.banking.asyncapi.generator.core.model.security.OAuthFlows
import com.tietoevry.banking.asyncapi.generator.core.model.security.SecurityScheme
import com.tietoevry.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext

class SecuritySchemeParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, SecuritySchemeInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseList(parserNode: ParserNode): List<SecuritySchemeInterface> = buildList {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            add(parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): SecuritySchemeInterface {
        parserNode.optional($$"$ref")?.coerce<String>()?.let { reference ->
            return SecuritySchemeInterface.SecuritySchemeReference(
                Reference(
                    ref = reference
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        val type = parserNode.mandatory("type").coerce<String>()
        val description = parserNode.optional("description")?.coerce<String>()
        val nameField = parserNode.optional("name")?.coerce<String>()
        val inField = parserNode.optional("in")?.coerce<String>()
        val scheme = parserNode.optional("scheme")?.coerce<String>()
        val bearerFormat = parserNode.optional("bearerFormat")?.coerce<String>()
        val openIdConnectUrl = parserNode.optional("openIdConnectUrl")?.coerce<String>()
        val flows = parserNode.optional("flows")?.let(::parseFlows)
        val scopes = parserNode.optional("scopes")?.coerce<List<String>>()
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
        parserNode.coerce<Map<*, *>>()
        return OAuthFlows(
            implicit = parserNode.optional("implicit")?.let(::parseFlow),
            password = parserNode.optional("password")?.let(::parseFlow),
            clientCredentials = parserNode.optional("clientCredentials")?.let(::parseFlow),
            authorizationCode = parserNode.optional("authorizationCode")?.let(::parseFlow),
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun parseFlow(parserNode: ParserNode): OAuthFlow {
        val authorizationUrl = parserNode.optional("authorizationUrl")?.coerce<String>()
        val tokenUrl = parserNode.optional("tokenUrl")?.coerce<String>()
        val refreshUrl = parserNode.optional("refreshUrl")?.coerce<String>()
        val availableScopes = parserNode.optional("availableScopes")?.coerce<Map<String, String>>()
        return OAuthFlow(
            authorizationUrl = authorizationUrl,
            tokenUrl = tokenUrl,
            refreshUrl = refreshUrl,
            availableScopes = availableScopes
        ).also { asyncApiContext.register(it, parserNode) }
    }
}
