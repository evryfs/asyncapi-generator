package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class BundledDocumentApprovalTest {

    private val bundlerFixtures = BundlerFixtures()

    @TempDir
    private lateinit var tempDir: Path

    @Test
    fun `bundles a transitive multi-file messaging contract`() {
        val bundledFile =
            bundleToTemporaryFile(
                sourcePath = "bundler/multi/asyncapi_multifile_example_main.yaml",
                scenario = "multi-file-messaging-contract",
            )

        bundlerFixtures.validatedDocument(bundledFile)

        val bundledText = bundledFile.readText()
        assertFalse(bundledText.contains("asyncapi_multifile_example_"))

        BundlerApprovals.verify(
            generated = bundledText,
            scenario = "multi-file-messaging-contract",
        )
    }

    @Test
    fun `preserves required operation and channel references while bundling their targets`() {
        val bundledFile =
            bundleToTemporaryFile(
                sourcePath = "bundler/approval/topology-preserving-operation/main.yaml",
                scenario = "topology-preserving-operation",
            )

        val bundledDocument = bundlerFixtures.validatedDocument(bundledFile)
        val ordersChannel = assertIs<ChannelInterface.ChannelInline>(bundledDocument.channels?.get("orders"))
        val orderServers = assertNotNull(ordersChannel.channel.servers)
        assertEquals(1, orderServers.size)
        assertEquals("#/servers/kafka", orderServers.single().ref)

        val sendOrder = assertIs<OperationInterface.OperationInline>(bundledDocument.operations?.get("sendOrder"))
        assertEquals("#/channels/orders", assertNotNull(sendOrder.operation.channel).ref)
        val operationMessages = assertNotNull(sendOrder.operation.messages)
        assertEquals(1, operationMessages.size)
        assertEquals("#/channels/orders/messages/orderCreated", operationMessages.single().ref)

        val bundledText = bundledFile.readText()
        assertFalse(bundledText.contains("messages.yaml"))
        assertFalse(bundledText.contains("schemas.yaml"))

        BundlerApprovals.verify(
            generated = bundledText,
            scenario = "topology-preserving-operation",
        )
    }

    private fun bundleToTemporaryFile(
        sourcePath: String,
        scenario: String,
    ): File {
        val bundledDocument = bundlerFixtures.bundledDocument(sourcePath)
        val outputFile = tempDir.resolve("$scenario.yaml").toFile()
        AsyncApiRegistry.writeYaml(outputFile, bundledDocument)
        return outputFile
    }
}
