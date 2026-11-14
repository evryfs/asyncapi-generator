package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.servers.Server
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReferenceBundlerTest {

    @Test
    fun `inlineIfUnvisited marks an unvisited reference as inline`() {
        val reference = Reference("#/components/externalDocs/api")

        val bundled = ReferenceBundler.inlineIfUnvisited(reference, BundlingContext.empty())

        assertTrue(bundled.inline)
        assertFalse(reference.inline, "the input reference is not changed")
    }

    @Test
    fun `inlineIfUnvisited keeps a visited reference unchanged`() {
        val reference = Reference("#/components/externalDocs/api")

        val bundled = ReferenceBundler.inlineIfUnvisited(reference, BundlingContext.empty().enter(reference))

        assertSame(reference, bundled)
        assertFalse(bundled.inline)
    }

    @Test
    fun `bundleReferencedModel bundles and inlines an unvisited referenced model`() {
        val reference = Reference(
            ref = "#/components/servers/production",
            model = Server(host = "kafka.example.com", protocol = "kafka"),
        )

        val bundled = ReferenceBundler.bundleReferencedModel<Server>(reference, BundlingContext.empty()) { server, context ->
            assertTrue(context.hasVisited(reference))
            server.copy(description = "Bundled server")
        }

        assertTrue(bundled.inline)
        assertEquals(
            Server(host = "kafka.example.com", protocol = "kafka", description = "Bundled server"),
            bundled.model,
        )
        assertFalse(reference.inline, "the input reference is not changed")
        assertEquals(Server(host = "kafka.example.com", protocol = "kafka"), reference.model)
    }

    @Test
    fun `bundleReferencedModel keeps a visited referenced model unchanged`() {
        val model = Server(host = "kafka.example.com", protocol = "kafka")
        val reference = Reference(
            ref = "#/components/servers/production",
            model = model,
        )

        val bundled = ReferenceBundler.bundleReferencedModel<Server>(reference, BundlingContext.empty().enter(reference)) { server, _ ->
            server.copy(description = "Should not be applied")
        }

        assertSame(reference, bundled)
        assertFalse(bundled.inline)
        assertSame(model, bundled.model)
    }
}
