package dev.banking.asyncapi.generator.core.bundler.tags

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.tags.TagInterface

/**
 * Bundles tag objects and references.
 */
internal class TagBundler {
    fun bundleMap(
        tags: Map<String, TagInterface>?,
        scope: BundlingScope,
    ): Map<String, TagInterface>? =
        tags?.mapValues { (_, tag) ->
            bundle(tag, scope)
        }

    fun bundleList(
        tags: List<TagInterface>?,
        scope: BundlingScope,
    ): List<TagInterface>? =
        tags?.map { tag ->
            bundle(tag, scope)
        }

    fun bundle(
        tag: TagInterface,
        scope: BundlingScope,
    ): TagInterface =
        when (tag) {
            is TagInterface.TagReference -> {
                tag.copy(reference = ReferenceBundler.inlineIfUnvisited(tag.reference, scope))
            }
            else -> tag
        }
}
