package dev.banking.asyncapi.generator.core.bundler.servers

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.security.SecuritySchemeBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface

/**
 * Bundles server objects and references.
 */
internal class ServerBundler {
    private val tagBundler = TagBundler()
    private val externalDocsBundler = ExternalDocsBundler()
    private val securitySchemeBundler = SecuritySchemeBundler()
    private val serverVariableBundler = ServerVariableBundler()
    private val bindingBundler = BindingBundler()

    fun bundleServers(
        servers: Map<String, ServerInterface>?,
        scope: BundlingScope,
    ): Map<String, ServerInterface>? {
        if (servers == null) return null

        return servers.mapValues { (_, serverInterface) ->
            when (serverInterface) {
                is ServerInterface.ServerInline ->
                    ServerInterface.ServerInline(
                        bundleServer(serverInterface.server, scope),
                    )

                is ServerInterface.ServerReference -> {
                    serverInterface.copy(
                        reference =
                            ReferenceBundler.bundleReferencedModel<Server>(
                                reference = serverInterface.reference,
                                scope = scope,
                            ) { server, nextScope ->
                                bundleServer(server, nextScope)
                            },
                    )
                }
            }
        }
    }

    fun bundleServer(
        server: Server,
        scope: BundlingScope,
    ): Server {
        val bundledVariables = serverVariableBundler.bundleMap(server.variables, scope)
        val bundledSecurity = securitySchemeBundler.bundleList(server.security, scope)
        val bundledTags = tagBundler.bundleList(server.tags, scope)
        val bundledExternalDocs = server.externalDocs?.let { externalDocsBundler.bundle(it, scope) }
        val bundledBindings = bindingBundler.bundleMap(server.bindings, scope)
        return server.copy(
            variables = bundledVariables,
            security = bundledSecurity,
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            bindings = bundledBindings,
        )
    }
}
