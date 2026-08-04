package dev.banking.asyncapi.generator.core.bundler.info

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.model.info.Info

/**
 * Bundles info metadata references.
 *
 * Expected behavior is covered by:
 * - `InfoBundlerTest`
 */
internal class InfoBundler {

    private val tagBundler = TagBundler()
    private val externalDocsBundler = ExternalDocsBundler()
    fun bundle(info: Info, context: BundlingContext): Info {
        val bundledTags = tagBundler.bundleList(info.tags, context)
        val bundledExternalDocs = info.externalDocs?.let { externalDocsBundler.bundle(it, context) }
        return info.copy(
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
        )
    }
}
