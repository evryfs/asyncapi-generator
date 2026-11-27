package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class BundlingWalkTest {
    private val walk = BundlingWalk()

    @Test
    fun `a reference to a model without members becomes an inline copy`() {
        val reference = Reference("#/components/externalDocs/api", model = ExternalDoc(url = "https://example.com"))

        val bundled =
            walk.member(
                ExternalDocInterface.ExternalDocReference(reference),
                walk.externalDoc,
                BundlingRun().rootScope(),
            )

        assertTrue(assertIs<ExternalDocInterface.ExternalDocReference>(bundled).reference.inline)
        assertFalse(reference.inline, "the input reference is not changed")
    }

    @Test
    fun `a reference already followed on the path stays a reference`() {
        val reference = Reference("#/components/externalDocs/api", model = ExternalDoc(url = "https://example.com"))
        val node = ExternalDocInterface.ExternalDocReference(reference)

        val bundled = walk.member(node, walk.externalDoc, BundlingRun().rootScope().enter(reference))

        assertSame(node, bundled)
    }

    @Test
    fun `a reference to a model with members becomes an inline copy of the bundled model`() {
        val reference =
            Reference(
                ref = "#/components/servers/production",
                model = Server(host = "kafka.example.com", protocol = "kafka"),
            )
        val kind =
            MemberKind(ServerInterface::ServerInline, ServerInterface::ServerReference) { server, scope ->
                assertTrue(scope.hasVisited(reference))
                server.copy(description = "Bundled server")
            }

        val bundled = walk.member(ServerInterface.ServerReference(reference), kind, BundlingRun().rootScope())

        val bundledReference = assertIs<ServerInterface.ServerReference>(bundled).reference
        assertTrue(bundledReference.inline)
        assertEquals(
            Server(host = "kafka.example.com", protocol = "kafka", description = "Bundled server"),
            bundledReference.model,
        )
        assertFalse(reference.inline, "the input reference is not changed")
        assertEquals(Server(host = "kafka.example.com", protocol = "kafka"), reference.model)
    }

    @Test
    fun `a model reached again through a followed reference is not bundled again`() {
        val model = Server(host = "kafka.example.com", protocol = "kafka")
        val reference = Reference(ref = "#/components/servers/production", model = model)
        val node = ServerInterface.ServerReference(reference)
        val kind =
            MemberKind(ServerInterface::ServerInline, ServerInterface::ServerReference) { server, _ ->
                server.copy(description = "Should not be applied")
            }

        val bundled = walk.member(node, kind, BundlingRun().rootScope().enter(reference))

        assertSame(node, bundled)
        assertSame(model, reference.model)
    }
}
