package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiBundlingException
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AsyncApiBundlerTest {

    private val asyncApiContext = AsyncApiContext()
    private val bundler = AsyncApiBundler()
    private val bundlerFixtures = BundlerFixtures(asyncApiContext)

    @Test
    fun `bundling preserves a self-contained single-file document`() {
        val original = bundlerFixtures.validatedDocument(
            TestResources.file("asyncapi_kafka_single_file_example.yaml"),
        )

        val bundled = bundler.bundle(original)

        assertEquals(original.asyncapi, bundled.asyncapi)
        assertEquals(original.info, bundled.info)
        assertEquals(original.channels?.size, bundled.channels?.size)
        assertEquals(original.components, bundled.components)
    }

    @Test
    fun `bundling traverses representative multi-file channel, message, and schema references`() {
        val bundled = bundlerFixtures.bundledDocument("bundler/multi/asyncapi_multifile_example_main.yaml")

        val externalChannelReference =
            bundled.channels!!["externalAuditChannel"] as ChannelInterface.ChannelReference
        assertTrue(externalChannelReference.reference.inline)
        assertIs<Channel>(externalChannelReference.reference.model)

        val externalChannel = externalChannelReference.reference.model as Channel
        val externalMessage =
            externalChannel.messages!!["externalAuditMessage"] as MessageInterface.MessageInline
        val externalPayload = externalMessage.message.payload as SchemaInterface.SchemaReference
        val externalSchema = externalPayload.reference.model as Schema
        assertEquals("Audit Metadata", externalSchema.title)
        assertTrue(externalSchema.properties!!.containsKey("timestamp"))
        assertTrue(externalSchema.properties.containsKey("actor"))
        assertTrue(externalSchema.properties.containsKey("reason"))
        assertIs<SchemaInterface.SchemaInline>(externalSchema.properties["timestamp"])

        val channelReference = bundled.channels["testChannel"] as ChannelInterface.ChannelReference
        val channel = channelReference.reference.model as Channel
        val message = channel.messages!!["testMessage"] as MessageInterface.MessageInline
        val payload = message.message.payload as SchemaInterface.SchemaInline
        assertIs<SchemaInterface.SchemaInline>(payload.schema.properties!!["id"])
    }

    @Test
    fun `bundling preserves the specification server pathname in yaml and json`(
        @TempDir tempDir: Path,
    ) {
        val document = AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Pathname API", version = "1.0.0"),
            servers = mapOf(
                "production" to ServerInterface.ServerInline(
                    Server(
                        host = "api.example.com",
                        protocol = "https",
                        pathname = "/{environment}",
                    ),
                ),
            ),
        )
        val bundled = bundler.bundle(document)
        val yamlFile = tempDir.resolve("asyncapi.yaml").toFile()
        val jsonFile = tempDir.resolve("asyncapi.json").toFile()

        AsyncApiRegistry.writeYaml(yamlFile, bundled)
        AsyncApiRegistry.writeJson(jsonFile, bundled)

        listOf(yamlFile, jsonFile).forEach { outputFile ->
            assertTrue(outputFile.readText().contains("pathname"))
            assertFalse(outputFile.readText().contains("pathName"))
        }
    }

    @Test
    fun `bundling serializes a referenced Boolean schema as a YAML literal`(
        @TempDir tempDir: Path,
    ) {
        val booleanSchema = SchemaInterface.BooleanSchema(false)
        val schemaReference = Reference("#/components/schemas/Nothing", model = booleanSchema)
        val document = AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Boolean schema API", version = "1.0.0"),
            components = ComponentInterface.ComponentInline(
                Component(
                    schemas = mapOf(
                        "Nothing" to SchemaInterface.SchemaReference(schemaReference),
                    ),
                ),
            ),
        )
        val bundled = bundler.bundle(document)
        val yamlFile = tempDir.resolve("asyncapi.yaml").toFile()

        AsyncApiRegistry.writeYaml(yamlFile, bundled)

        val yaml = yamlFile.readText()
        assertTrue(yaml.contains("Nothing: false"))
        assertFalse(yaml.contains("modelForSerialization"))
    }

    @Test
    fun `bundling circular references should not cause stack overflow`() {
        val bundled = bundlerFixtures.bundledDocument("bundler/circular/asyncapi_bundler_circular.yaml")

        val components = bundled.components as ComponentInterface.ComponentInline
        val nodeA = components.component.schemas!!["NodeA"] as SchemaInterface.SchemaInline
        val child = nodeA.schema.properties!!["child"] as SchemaInterface.SchemaReference
        assertEquals("#/components/schemas/NodeB", child.reference.ref)
        assertIs<Schema>(child.reference.model)
        assertTrue(child.reference.inline)

        val schemas = components.component.schemas!!
        val nodeB = schemas["NodeB"] as SchemaInterface.SchemaInline
        val parent = nodeB.schema.properties!!["parent"] as SchemaInterface.SchemaReference
        assertEquals("#/components/schemas/NodeA", parent.reference.ref)
        assertIs<Schema>(parent.reference.model)
        assertTrue(parent.reference.inline)
    }

    @Test
    fun `bundling promotes a recursive external schema and produces standalone yaml and json`(
        @TempDir tempDir: Path,
    ) {
        val bundled = bundlerFixtures.bundledDocument("bundler/recursive-external/asyncapi.yaml")

        val channel = bundled.channels!!["treeEvents"] as ChannelInterface.ChannelInline
        val message = channel.channel.messages!!["treeUpdated"] as MessageInterface.MessageInline
        val payload = message.message.payload as SchemaInterface.SchemaReference
        assertEquals("#/components/schemas/TreeNode", payload.reference.ref)

        val components = bundled.components as ComponentInterface.ComponentInline
        val treeNode = components.component.schemas!!["TreeNode"] as SchemaInterface.SchemaInline
        val children = treeNode.schema.properties!!["children"] as SchemaInterface.SchemaInline
        val childItems = children.schema.items as SchemaInterface.SchemaReference
        assertEquals("#/components/schemas/TreeNode", childItems.reference.ref)

        val yamlFile = tempDir.resolve("asyncapi.yaml").toFile()
        val jsonFile = tempDir.resolve("asyncapi.json").toFile()
        AsyncApiRegistry.writeYaml(yamlFile, bundled)
        AsyncApiRegistry.writeJson(jsonFile, bundled)

        listOf(yamlFile, jsonFile).forEach { outputFile ->
            assertTrue(outputFile.readText().contains("#/components/schemas/TreeNode"))
            assertFalse(outputFile.readText().contains("schemas.yaml"))
            assertNotNull(BundlerFixtures().validatedDocument(outputFile))
        }
    }

    @Test
    fun `bundling promotes mutually recursive schemas from an external schema document`() {
        val bundled = bundlerFixtures.bundledDocument("bundler/recursive-external-mutual/asyncapi.yaml")

        val components = bundled.components as ComponentInterface.ComponentInline
        val schemas = components.component.schemas!!
        assertEquals(setOf("ParentNode", "ChildNode"), schemas.keys)

        val parent = schemas["ParentNode"] as SchemaInterface.SchemaInline
        val childReference = parent.schema.properties!!["child"] as SchemaInterface.SchemaReference
        assertEquals("#/components/schemas/ChildNode", childReference.reference.ref)

        val child = schemas["ChildNode"] as SchemaInterface.SchemaInline
        val parentReference = child.schema.properties!!["parent"] as SchemaInterface.SchemaReference
        assertEquals("#/components/schemas/ParentNode", parentReference.reference.ref)
    }

    @Test
    fun `bundling rejects a promoted schema name that conflicts with a root schema`() {
        val error = assertFailsWith<AsyncApiBundlingException.PromotedSchemaNameCollision> {
            bundlerFixtures.bundledDocument("bundler/recursive-external-collision/root-schema-collision.yaml")
        }
        assertTrue(error.message!!.contains("recursive external schema 'TreeNode'"))
        assertTrue(error.message!!.contains("Existing schema: #/components/schemas/TreeNode"))
        assertTrue(error.message!!.contains("external-tree.yaml#/components/schemas/TreeNode"))
    }

    @Test
    fun `bundling rejects distinct promoted schemas with the same name`() {
        val error = assertFailsWith<AsyncApiBundlingException.PromotedSchemaNameCollision> {
            bundlerFixtures.bundledDocument("bundler/recursive-external-collision/external-schema-collision.yaml")
        }
        assertTrue(error.message!!.contains("recursive external schema 'SharedNode'"))
        assertTrue(error.message!!.contains("first-node.yaml#/components/schemas/SharedNode"))
        assertTrue(error.message!!.contains("second-node.yaml#/components/schemas/SharedNode"))
    }
}
