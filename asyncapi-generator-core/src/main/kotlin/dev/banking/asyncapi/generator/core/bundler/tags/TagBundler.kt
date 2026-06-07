package dev.banking.asyncapi.generator.core.bundler.tags

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.tags.TagInterface

/**
 * Bundles tag objects and references.
 *
 * Expected behavior is covered by:
 * - `TagBundlerTest`
 */
class TagBundler {

    fun bundleMap(tags: Map<String, TagInterface>?, visited: Set<String>): Map<String, TagInterface>? =
        bundleMap(tags, BundlingContext.from(visited))

    fun bundleMap(tags: Map<String, TagInterface>?, context: BundlingContext): Map<String, TagInterface>? =
        tags?.mapValues { (_, tag) ->
            bundle(tag, context)
        }

    fun bundleList(tags: List<TagInterface>?, visited: Set<String>): List<TagInterface>? =
        bundleList(tags, BundlingContext.from(visited))

    fun bundleList(tags: List<TagInterface>?, context: BundlingContext): List<TagInterface>? =
        tags?.map { tag ->
            bundle(tag, context)
        }

    fun bundle(tag: TagInterface, context: BundlingContext): TagInterface =
        when (tag) {
            is TagInterface.TagReference -> {
                ReferenceBundler.inlineIfUnvisited(tag.reference, context)
                tag
            }
            else -> tag
        }
}
