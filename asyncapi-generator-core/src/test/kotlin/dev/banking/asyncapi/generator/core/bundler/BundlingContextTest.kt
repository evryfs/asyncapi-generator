package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class BundlingContextTest {

    @Test
    fun `empty context has no visited references`() {
        val reference = Reference("#/components/schemas/User")

        val context = BundlingContext.empty()

        assertFalse(context.hasVisited(reference))
    }

    @Test
    fun `enter returns a context with the visited reference`() {
        val reference = Reference("#/components/schemas/User")
        val original = BundlingContext.empty()

        val next = original.enter(reference)

        assertNotSame(original, next)
        assertFalse(original.hasVisited(reference))
        assertTrue(next.hasVisited(reference))
    }

    @Test
    fun `references from different source documents have distinct traversal identities`() {
        val first = Reference("#/components/schemas/User", sourceId = "first.yaml")
        val second = Reference("#/components/schemas/User", sourceId = "second.yaml")

        val context = BundlingContext.empty().enter(first)

        assertTrue(context.hasVisited(first))
        assertFalse(context.hasVisited(second))
    }
}
