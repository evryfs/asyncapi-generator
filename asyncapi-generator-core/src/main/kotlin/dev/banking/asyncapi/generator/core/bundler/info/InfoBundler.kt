package dev.banking.asyncapi.generator.core.bundler.info

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.model.info.Info

/**
 * Bundles info metadata references.
 */
internal class InfoBundler {
    private val tagBundler = TagBundler()
    private val externalDocsBundler = ExternalDocsBundler()

    fun bundle(
        info: Info,
        scope: BundlingScope,
    ): Info {
        val bundledTags = tagBundler.bundleList(info.tags, scope)
        val bundledExternalDocs = info.externalDocs?.let { externalDocsBundler.bundle(it, scope) }
        return info.copy(
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
        )
    }
}
