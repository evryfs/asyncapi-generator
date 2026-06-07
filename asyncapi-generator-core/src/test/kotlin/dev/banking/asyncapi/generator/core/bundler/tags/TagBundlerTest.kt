package dev.banking.asyncapi.generator.core.bundler.tags

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TagBundlerTest {

    private val bundler = TagBundler()

    @Test
    fun `bundleList marks an unvisited tag reference as inline`() {
        val reference = Reference("#/components/tags/audit")
        val tag = TagInterface.TagReference(reference)

        val bundled = bundler.bundleList(listOf(tag), BundlingContext.empty())

        assertThat(bundled).containsExactly(tag)
        assertThat(reference.inline).isTrue()
    }

    @Test
    fun `bundleMap keeps a visited tag reference unchanged`() {
        val reference = Reference("#/components/tags/audit")
        val tag = TagInterface.TagReference(reference)

        val bundled = bundler.bundleMap(mapOf("audit" to tag), BundlingContext.empty().enter(reference))

        assertThat(bundled).containsEntry("audit", tag)
        assertThat(reference.inline).isFalse()
    }
}
