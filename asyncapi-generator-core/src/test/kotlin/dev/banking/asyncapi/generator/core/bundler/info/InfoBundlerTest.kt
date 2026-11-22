package dev.banking.asyncapi.generator.core.bundler.info

import dev.banking.asyncapi.generator.core.bundler.BundlingRun
import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InfoBundlerTest {
    private val walk = BundlingWalk()

    @Test
    fun `bundle marks unvisited info references as inline`() {
        val tagReference = Reference("#/components/tags/public", model = Tag(name = "public"))
        val externalDocReference =
            Reference("#/components/externalDocs/api", model = ExternalDoc(url = "https://example.com"))
        val info =
            Info(
                title = "User API",
                version = "1.0.0",
                tags = listOf(TagInterface.TagReference(tagReference)),
                externalDocs = ExternalDocInterface.ExternalDocReference(externalDocReference),
            )

        val bundled = walk.info.bundle(info, BundlingRun().rootScope())

        assertEquals(listOf(TagInterface.TagReference(tagReference)), bundled.tags)
        assertEquals(ExternalDocInterface.ExternalDocReference(externalDocReference), bundled.externalDocs)
        assertTrue(assertIs<TagInterface.TagReference>(bundled.tags!!.single()).reference.inline)
        assertTrue(assertIs<ExternalDocInterface.ExternalDocReference>(bundled.externalDocs).reference.inline)
        assertFalse(tagReference.inline, "the input reference is not changed")
        assertFalse(externalDocReference.inline, "the input reference is not changed")
    }
}
