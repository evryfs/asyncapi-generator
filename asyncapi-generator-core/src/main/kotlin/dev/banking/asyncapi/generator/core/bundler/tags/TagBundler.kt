package dev.banking.asyncapi.generator.core.bundler.tags

import dev.banking.asyncapi.generator.core.model.tags.TagInterface

class TagBundler {

    fun bundleMap(tags: Map<String, TagInterface>?, visited: Set<String>): Map<String, TagInterface>? =
        tags?.mapValues { (_, tag) ->
            when (tag) {
                is TagInterface.TagReference -> {
                    val ref = tag.reference.ref
                    if (!visited.contains(ref)) {
                        tag.reference.inline()
                    }
                    tag
                }
                else -> tag
            }
        }

    fun bundleList(tags: List<TagInterface>?, visited: Set<String>): List<TagInterface>? =
        tags?.map { tag ->
            when (tag) {
                is TagInterface.TagReference -> {
                    val ref = tag.reference.ref
                    if (!visited.contains(ref)) {
                        tag.reference.inline()
                    }
                    tag
                }
                else -> tag
            }
        }
}
