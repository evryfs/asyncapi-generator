package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
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

    @Test
    fun `bundles a selected schema fragment from a foreign document`() {
        val bundledFile =
            bundleToTemporaryFile(
                sourcePath = "validator/schemas/external/asyncapi_external_selected_schema.yaml",
                scenario = "foreign-schema-fragment",
            )

        val bundledDocument = bundlerFixtures.validatedDocument(bundledFile)
        val components = assertIs<ComponentInterface.ComponentInline>(bundledDocument.components)
        val schemas = assertNotNull(components.component.schemas)
        assertEquals(setOf("LocalEnvelope"), schemas.keys)
        assertIs<SchemaInterface.SchemaInline>(schemas["LocalEnvelope"])

        val bundledText = bundledFile.readText()
        assertFalse(bundledText.contains("openapi:"))
        assertFalse(bundledText.contains("paths:"))
        assertFalse(bundledText.contains("UnreferencedOpenApiPayload"))
        assertFalse(bundledText.contains("foreign_container_with_unreferenced_invalid.yaml"))

        BundlerApprovals.verify(
            generated = bundledText,
            scenario = "foreign-schema-fragment",
        )
    }

    @Test
    fun `promotes mutually recursive external schemas to local components`() {
        val bundledFile =
            bundleToTemporaryFile(
                sourcePath = "bundler/recursive-external-mutual/asyncapi.yaml",
                scenario = "mutually-recursive-schemas",
            )

        val bundledDocument = bundlerFixtures.validatedDocument(bundledFile)
        val components = assertIs<ComponentInterface.ComponentInline>(bundledDocument.components)
        val schemas = assertNotNull(components.component.schemas)
        assertEquals(setOf("ParentNode", "ChildNode"), schemas.keys)

        val parentNode = assertIs<SchemaInterface.SchemaInline>(schemas["ParentNode"])
        val childReference =
            assertIs<SchemaInterface.SchemaReference>(assertNotNull(parentNode.schema.properties)["child"])
        assertEquals("#/components/schemas/ChildNode", childReference.reference.ref)

        val childNode = assertIs<SchemaInterface.SchemaInline>(schemas["ChildNode"])
        val parentReference =
            assertIs<SchemaInterface.SchemaReference>(assertNotNull(childNode.schema.properties)["parent"])
        assertEquals("#/components/schemas/ParentNode", parentReference.reference.ref)

        val bundledText = bundledFile.readText()
        assertFalse(bundledText.contains("schemas.yaml"))

        BundlerApprovals.verify(
            generated = bundledText,
            scenario = "mutually-recursive-schemas",
        )
    }

    @Test
    fun `bundles an external Kafka message key schema`() {
        val bundledFile =
            bundleToTemporaryFile(
                sourcePath = "generator/spring-kafka/single-message.yaml",
                scenario = "external-kafka-key-schema",
            )

        val bundledDocument = bundlerFixtures.validatedDocument(bundledFile)
        val components = assertIs<ComponentInterface.ComponentInline>(bundledDocument.components).component
        val message = assertIs<MessageInterface.MessageInline>(components.messages?.get("MyAccountUpdated")).message
        val kafkaBinding = assertIs<BindingInterface.BindingInline>(message.bindings?.get("kafka")).binding
        val keySchema = assertIs<SchemaInterface.SchemaInline>(kafkaBinding.kafkaKeySchema)
        assertEquals("object", keySchema.schema.type)
        assertEquals(setOf("institutionId", "accountId"), keySchema.schema.properties?.keys)

        val bundledText = bundledFile.readText()
        assertFalse(bundledText.contains("key-schemas.yaml"))

        BundlerApprovals.verify(
            generated = bundledText,
            scenario = "external-kafka-key-schema",
        )
    }

    @Test
    fun `bundles a representative document for external tool interoperability`() {
        val bundledFile =
            bundleToTemporaryFile(
                sourcePath = "bundler/approval/interoperability/main.yaml",
                scenario = "interoperability-contract",
            )

        val bundledDocument = bundlerFixtures.validatedDocument(bundledFile)
        val channel = assertIs<ChannelInterface.ChannelInline>(bundledDocument.channels?.get("lifecycleEvents")).channel
        assertEquals("#/servers/kafka", channel.servers?.single()?.ref)
        assertEquals(setOf("accountUpdated", "auditEvent", "treeUpdated"), channel.messages?.keys)

        val publish =
            assertIs<OperationInterface.OperationInline>(
                bundledDocument.operations?.get("publishLifecycleEvents"),
            ).operation
        assertEquals("#/channels/lifecycleEvents", publish.channel?.ref)
        assertEquals(
            listOf(
                "#/channels/lifecycleEvents/messages/accountUpdated",
                "#/channels/lifecycleEvents/messages/auditEvent",
                "#/channels/lifecycleEvents/messages/treeUpdated",
            ),
            publish.messages?.map { it.ref },
        )

        val accountUpdated = assertIs<MessageInterface.MessageInline>(channel.messages?.get("accountUpdated")).message
        val kafkaBinding = assertIs<BindingInterface.BindingInline>(accountUpdated.bindings?.get("kafka")).binding
        assertIs<SchemaInterface.SchemaInline>(kafkaBinding.kafkaKeySchema)

        val components = assertIs<ComponentInterface.ComponentInline>(bundledDocument.components).component
        assertEquals(setOf("ParentNode", "ChildNode"), components.schemas?.keys)

        val bundledText = bundledFile.readText()
        val externalReferences =
            Regex("""${'$'}ref:\s*[\"']?([^\"'\s]+)""")
                .findAll(bundledText)
                .map { it.groupValues[1] }
                .filterNot { it.startsWith("#") }
                .toList()
        assertEquals(emptyList(), externalReferences)

        BundlerApprovals.verify(
            generated = bundledText,
            scenario = "interoperability-contract",
        )
    }

    @Test
    fun `bundles reusable objects from an external component catalog`() {
        val bundledFile =
            bundleToTemporaryFile(
                sourcePath = "bundler/approval/external-component-catalog/main.yaml",
                scenario = "external-component-catalog",
            )

        val bundledDocument = bundlerFixtures.validatedDocument(bundledFile)
        val components = assertIs<ComponentInterface.ComponentInline>(bundledDocument.components).component
        assertIs<MessageInterface.MessageInline>(components.messages?.get("AuditEvent"))
        assertIs<OperationTraitInterface.OperationTraitInline>(components.operationTraits?.get("Audited"))
        assertIs<OperationReplyInterface.OperationReplyInline>(components.replies?.get("Accepted"))
        assertIs<SecuritySchemeInterface.SecuritySchemeInline>(components.securitySchemes?.get("Credentials"))
        assertIs<BindingInterface.BindingInline>(components.channelBindings?.get("AuditTopic"))

        val operation = assertIs<OperationInterface.OperationInline>(components.operations?.get("PublishAudit"))
        assertEquals(
            "#/components/channels/AuditEvents",
            assertNotNull(operation.operation.channel).ref,
        )

        val bundledText = bundledFile.readText()
        assertFalse(bundledText.contains("catalog.yaml"))

        BundlerApprovals.verify(
            generated = bundledText,
            scenario = "external-component-catalog",
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
