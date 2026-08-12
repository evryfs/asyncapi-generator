package dev.banking.asyncapi.generator.core.bundler.servers

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ServerBundlerTest {

    private val bundler = ServerBundler()

    @Test
    fun `bundleServers bundles and inlines an unvisited server reference`() {
        val bindingReference = Reference(
            "#/components/serverBindings/kafka",
            model = Binding(content = emptyMap()),
        )
        val server = Server(
            host = "kafka.example.com",
            protocol = "kafka",
            bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
        )
        val serverReference = Reference("#/components/servers/production", model = server)
        val serverInterface = ServerInterface.ServerReference(serverReference)

        val bundled = bundler.bundleServers(mapOf("production" to serverInterface), BundlingContext.empty())

        assertThat(bundled).containsEntry("production", serverInterface)
        assertThat(serverReference.inline).isTrue()
        assertThat(serverReference.model)
            .isEqualTo(server.copy(bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference))))
        assertThat(bindingReference.inline).isTrue()
    }

}
