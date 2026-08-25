package dev.banking.asyncapi.generator.core.bundler.info

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InfoBundlerTest {

    private val bundler = InfoBundler()

    @Test
    fun `bundle marks unvisited info references as inline`() {
        val tagReference = Reference("#/components/tags/public")
        val externalDocReference = Reference("#/components/externalDocs/api")
        val info = Info(
            title = "User API",
            version = "1.0.0",
            tags = listOf(TagInterface.TagReference(tagReference)),
            externalDocs = ExternalDocInterface.ExternalDocReference(externalDocReference),
        )

        val bundled = bundler.bundle(info, BundlingContext.empty())

        assertEquals(listOf(TagInterface.TagReference(tagReference)), bundled.tags)
        assertEquals(ExternalDocInterface.ExternalDocReference(externalDocReference), bundled.externalDocs)
        assertTrue(tagReference.inline)
        assertTrue(externalDocReference.inline)
    }
}
