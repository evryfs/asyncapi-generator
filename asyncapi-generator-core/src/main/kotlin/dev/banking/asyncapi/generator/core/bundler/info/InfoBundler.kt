package dev.banking.asyncapi.generator.core.bundler.info

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.info.Info

/**
 * Bundles the members of the info object.
 *
 * @param walk bundles the members of the info
 */
internal class InfoBundler(
    private val walk: BundlingWalk,
) {
    /** Returns [info] with its members bundled. */
    fun bundle(
        info: Info,
        scope: BundlingScope,
    ): Info =
        info.copy(
            tags = walk.members(info.tags, walk.tag, scope),
            externalDocs = walk.member(info.externalDocs, walk.externalDoc, scope),
        )
}
