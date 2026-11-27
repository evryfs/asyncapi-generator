package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

internal class BundlingScopeTest {
    @Test
    fun `empty scope has no visited references`() {
        val reference = Reference("#/components/schemas/User")

        val scope = BundlingRun().rootScope()

        assertFalse(scope.hasVisited(reference))
    }

    @Test
    fun `enter returns a scope with the visited reference`() {
        val reference = Reference("#/components/schemas/User")
        val original = BundlingRun().rootScope()

        val next = original.enter(reference)

        assertNotSame(original, next)
        assertFalse(original.hasVisited(reference))
        assertTrue(next.hasVisited(reference))
    }

    @Test
    fun `references from different source documents have distinct traversal identities`() {
        val first = Reference("#/components/schemas/User", sourceId = "first.yaml")
        val second = Reference("#/components/schemas/User", sourceId = "second.yaml")

        val scope = BundlingRun().rootScope().enter(first)

        assertTrue(scope.hasVisited(first))
        assertFalse(scope.hasVisited(second))
    }
}
