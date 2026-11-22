package dev.banking.asyncapi.generator.core.bundler.servers

import dev.banking.asyncapi.generator.core.bundler.BundlingRun
import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ServerBundlerTest {
    private val walk = BundlingWalk()

    @Test
    fun `bundleServers bundles and inlines an unvisited server reference`() {
        val bindingReference =
            Reference(
                "#/components/serverBindings/kafka",
                model = Binding(content = emptyMap()),
            )
        val server =
            Server(
                host = "kafka.example.com",
                protocol = "kafka",
                bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
            )
        val serverReference = Reference("#/components/servers/production", model = server)
        val serverInterface = ServerInterface.ServerReference(serverReference)

        val bundled = walk.members(mapOf("production" to serverInterface), walk.server, BundlingRun().rootScope())

        val bundledServer = assertIs<ServerInterface.ServerReference>(bundled!!["production"])
        assertTrue(bundledServer.reference.inline)
        val bundledModel = assertIs<Server>(bundledServer.reference.model)
        assertTrue(assertIs<BindingInterface.BindingReference>(bundledModel.bindings!!["kafka"]).reference.inline)
        assertFalse(serverReference.inline, "the input reference is not changed")
        assertFalse(bindingReference.inline, "the input reference is not changed")
    }
}
