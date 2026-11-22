package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.parameters.PARAMETER_PLACEHOLDER
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER_VARIABLE
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_HOST_PROTOCOL
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_HOST_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_NAME_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_PROTOCOL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_UNDEFINED
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.util.requireNonBlank
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Server Object and walks its variables, security, tags, external documentation and
 * bindings.
 *
 * @param asyncApiContext the load the servers belong to
 */
internal class ServerRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<Server> {
    override fun check(
        node: Server,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        validateHost(node, path, walk)
        validateProtocol(node, path, walk)
        node.variables?.forEach { (name, variable) ->
            walk.member(variable, SERVER_VARIABLE, path.named("Server Variable", name))
        }
        walk.securitySchemes(node.security, path)
        walk.tags(node.tags, path)
        walk.externalDocs(node.externalDocs, path)
        walk.bindings(node.bindings, path)
    }

    private fun validateHost(
        node: Server,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val host = node.host
        val hostSource = asyncApiContext.modelTracking.getSourceLocation(node, node::host)
        if (walk.results.requireNonBlank(host, "host", path, SERVER_HOST_REQUIRED, hostSource) && "://" in host) {
            walk.results.warn(
                SERVER_HOST_PROTOCOL,
                "$path host '$host' includes scheme/protocol. 'host' should typically be the hostname " +
                    "(e.g. api.example.com) as protocol is defined separately.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::host),
            )
        }
        validateVariables(node, path, walk)
    }

    private fun validateVariables(
        node: Server,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val definedVars = node.variables?.keys ?: emptySet()
        val hostVars = placeholders(node.host)
        val pathVars = node.pathname?.let(::placeholders) ?: emptySet()
        val missingHostVars = hostVars - definedVars
        if (missingHostVars.isNotEmpty()) {
            walk.results.error(
                SERVER_VARIABLE_UNDEFINED,
                "$path host uses variables $missingHostVars which are not defined in 'variables'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::host),
            )
        }
        val missingPathVars = pathVars - definedVars
        if (missingPathVars.isNotEmpty()) {
            walk.results.error(
                SERVER_VARIABLE_UNDEFINED,
                "$path pathname uses variables $missingPathVars which are not defined in 'variables'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::pathname),
            )
        }
    }

    private fun placeholders(text: String): Set<String> =
        PARAMETER_PLACEHOLDER.findAll(text).map { it.groupValues[1] }.toSet()

    private fun validateProtocol(
        node: Server,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        walk.results.requireNonBlank(
            node.protocol,
            "protocol",
            path,
            SERVER_PROTOCOL_REQUIRED,
            asyncApiContext.modelTracking.getSourceLocation(node, node::protocol),
        )
    }

    companion object {
        /** Reports a [name] of the `servers` map that is not a reusable object name. */
        fun checkName(
            name: String,
            server: ServerInterface,
            path: ValidationPath,
            walk: ValidationWalk,
        ) {
            if (!ValidationFormats.isReusableObjectName(name)) {
                walk.results.error(
                    SERVER_NAME_FORMAT,
                    "$path name must contain only letters, digits, underscores, or hyphens.",
                    sourceLocation = walk.modelTracking.getSourceLocation(server.held()),
                )
            }
        }
    }
}
