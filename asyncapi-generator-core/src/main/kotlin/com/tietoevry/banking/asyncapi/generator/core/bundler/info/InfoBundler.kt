package com.tietoevry.banking.asyncapi.generator.core.bundler.info

import com.tietoevry.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.tags.TagBundler
import com.tietoevry.banking.asyncapi.generator.core.model.info.Info

class InfoBundler {

    private val tagBundler: TagBundler = TagBundler()
    private val externalDocsBundler: ExternalDocsBundler = ExternalDocsBundler()

    fun bundle(info: Info, visited: Set<String>): Info {
        val bundledTags = tagBundler.bundleList(info.tags, visited)
        val bundledExternalDocs = info.externalDocs?.let { externalDocsBundler.bundle(it, visited) }
        return info.copy(
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
        )
    }
}
