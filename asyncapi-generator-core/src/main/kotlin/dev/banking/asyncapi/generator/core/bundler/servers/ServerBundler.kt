package dev.banking.asyncapi.generator.core.bundler.servers

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.servers.Server

/**
 * Bundles the members of a server.
 *
 * @param walk bundles the members of the server
 */
internal class ServerBundler(
    private val walk: BundlingWalk,
) {
    /** Returns [server] with its members bundled. */
    fun bundle(
        server: Server,
        scope: BundlingScope,
    ): Server =
        server.copy(
            variables = walk.members(server.variables, walk.serverVariable, scope),
            security = walk.members(server.security, walk.securityScheme, scope),
            tags = walk.members(server.tags, walk.tag, scope),
            externalDocs = walk.member(server.externalDocs, walk.externalDoc, scope),
            bindings = walk.members(server.bindings, walk.binding, scope),
        )
}
