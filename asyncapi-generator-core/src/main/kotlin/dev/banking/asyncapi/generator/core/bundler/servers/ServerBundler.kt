package dev.banking.asyncapi.generator.core.bundler.servers

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
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
        context: BundlingContext,
    ): Map<String, ServerInterface>? {
        if (servers == null) return null

        return servers.mapValues { (_, serverInterface) ->
            when (serverInterface) {
                is ServerInterface.ServerInline ->
                    ServerInterface.ServerInline(
                        bundleServer(serverInterface.server, context)
                    )

                is ServerInterface.ServerReference -> {
                    ReferenceBundler.bundleReferencedModel<Server>(
                        reference = serverInterface.reference,
                        context = context,
                    ) { server, nextContext ->
                        bundleServer(server, nextContext)
                    }
                    serverInterface
                }
            }
        }
    }
    fun bundleServer(server: Server, context: BundlingContext): Server {
        val bundledVariables = serverVariableBundler.bundleMap(server.variables, context)
        val bundledSecurity = securitySchemeBundler.bundleList(server.security, context)
        val bundledTags = tagBundler.bundleList(server.tags, context)
        val bundledExternalDocs = server.externalDocs?.let { externalDocsBundler.bundle(it, context) }
        val bundledBindings = bindingBundler.bundleMap(server.bindings, context)
        return server.copy(
            variables = bundledVariables,
            security = bundledSecurity,
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            bindings = bundledBindings
        )
    }
}
