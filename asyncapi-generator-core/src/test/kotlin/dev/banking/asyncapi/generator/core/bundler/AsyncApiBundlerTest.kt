package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiBundlingException
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertNotNull

class AsyncApiBundlerTest {

    private val asyncApiContext = AsyncApiContext()
    private val bundler = AsyncApiBundler()
    private val bundlerFixtures = BundlerFixtures(asyncApiContext)

    @Test
    fun asyncApiSingleFile() {
        val file = TestResources.file("asyncapi_kafka_single_file_example.yaml")
        val parsed = bundlerFixtures.validatedDocument(file)
        val result = bundler.bundle(parsed)
        val expected = expectedSingleFileBundled(file)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expected)
    }

    @Test
    fun asyncApiMultiFile() {
        val bundled = bundlerFixtures.bundledDocument("bundler/multi/asyncapi_multifile_example_main.yaml")
        AsyncApiRegistry.writeYaml(File("src/test/resources/bundler/bundled/asyncapi-bundled.yaml"), bundled)
    }

    @Test
    fun asyncApiMultiFileAssertions() {
        val result = bundlerFixtures.bundledDocument("bundler/multi/asyncapi_multifile_example_main.yaml")
        val expected = expectedMultiFileBundled()
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expected)
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
            assertThat(outputFile.readText())
                .contains("pathname")
                .doesNotContain("pathName")
        }
    }

    @Test
    fun `bundling circular references should not cause stack overflow`() {
        val bundled = bundlerFixtures.bundledDocument("bundler/circular/asyncapi_bundler_circular.yaml")
        assertNotNull(bundled, "Bundled document should not be null")
    }

    @Test
    fun `bundling promotes a recursive external schema and produces standalone yaml and json`(
        @TempDir tempDir: Path,
    ) {
        val bundled = bundlerFixtures.bundledDocument("bundler/recursive-external/asyncapi.yaml")

        val channel = bundled.channels!!["treeEvents"] as ChannelInterface.ChannelInline
        val message = channel.channel.messages!!["treeUpdated"] as MessageInterface.MessageInline
        val payload = message.message.payload as SchemaInterface.SchemaReference
        assertThat(payload.reference.ref).isEqualTo("#/components/schemas/TreeNode")

        val components = bundled.components as ComponentInterface.ComponentInline
        val treeNode = components.component.schemas!!["TreeNode"] as SchemaInterface.SchemaInline
        val children = treeNode.schema.properties!!["children"] as SchemaInterface.SchemaInline
        val childItems = children.schema.items as SchemaInterface.SchemaReference
        assertThat(childItems.reference.ref).isEqualTo("#/components/schemas/TreeNode")

        val yamlFile = tempDir.resolve("asyncapi.yaml").toFile()
        val jsonFile = tempDir.resolve("asyncapi.json").toFile()
        AsyncApiRegistry.writeYaml(yamlFile, bundled)
        AsyncApiRegistry.writeJson(jsonFile, bundled)

        listOf(yamlFile, jsonFile).forEach { outputFile ->
            assertThat(outputFile.readText())
                .contains("#/components/schemas/TreeNode")
                .doesNotContain("schemas.yaml")
            assertNotNull(BundlerFixtures().validatedDocument(outputFile))
        }
    }

    @Test
    fun `bundling promotes mutually recursive schemas from an external schema document`(
        @TempDir tempDir: Path,
    ) {
        val bundled = bundlerFixtures.bundledDocument("bundler/recursive-external-mutual/asyncapi.yaml")

        val components = bundled.components as ComponentInterface.ComponentInline
        val schemas = components.component.schemas!!
        assertThat(schemas.keys).containsExactly("ParentNode", "ChildNode")

        val parent = schemas["ParentNode"] as SchemaInterface.SchemaInline
        val childReference = parent.schema.properties!!["child"] as SchemaInterface.SchemaReference
        assertThat(childReference.reference.ref).isEqualTo("#/components/schemas/ChildNode")

        val child = schemas["ChildNode"] as SchemaInterface.SchemaInline
        val parentReference = child.schema.properties!!["parent"] as SchemaInterface.SchemaReference
        assertThat(parentReference.reference.ref).isEqualTo("#/components/schemas/ParentNode")

        val yamlFile = tempDir.resolve("asyncapi.yaml").toFile()
        val jsonFile = tempDir.resolve("asyncapi.json").toFile()
        AsyncApiRegistry.writeYaml(yamlFile, bundled)
        AsyncApiRegistry.writeJson(jsonFile, bundled)

        listOf(yamlFile, jsonFile).forEach { outputFile ->
            assertThat(outputFile.readText())
                .contains("#/components/schemas/ParentNode")
                .contains("#/components/schemas/ChildNode")
                .doesNotContain("schemas.yaml")
            assertNotNull(BundlerFixtures().validatedDocument(outputFile))
        }
    }

    @Test
    fun `bundling rejects a promoted schema name that conflicts with a root schema`() {
        assertThatThrownBy {
            bundlerFixtures.bundledDocument("bundler/recursive-external-collision/root-schema-collision.yaml")
        }
            .isInstanceOf(AsyncApiBundlingException.PromotedSchemaNameCollision::class.java)
            .hasMessageContaining("recursive external schema 'TreeNode'")
            .hasMessageContaining("Existing schema: #/components/schemas/TreeNode")
            .hasMessageContaining("external-tree.yaml#/components/schemas/TreeNode")
    }

    @Test
    fun `bundling rejects distinct promoted schemas with the same name`() {
        assertThatThrownBy {
            bundlerFixtures.bundledDocument("bundler/recursive-external-collision/external-schema-collision.yaml")
        }
            .isInstanceOf(AsyncApiBundlingException.PromotedSchemaNameCollision::class.java)
            .hasMessageContaining("recursive external schema 'SharedNode'")
            .hasMessageContaining("first-node.yaml#/components/schemas/SharedNode")
            .hasMessageContaining("second-node.yaml#/components/schemas/SharedNode")
    }

    @Test
    fun `bundling marks references as inline`() {
        val bundled = bundlerFixtures.bundledDocument("bundler/multi/asyncapi_multifile_example_main.yaml")

        val channelRef = bundled.channels!!["testChannel"] as ChannelInterface.ChannelReference
        val bundledChannel = channelRef.reference.model as Channel
        val messageInline = bundledChannel.messages!!["testMessage"] as MessageInterface.MessageInline

        assertThat(channelRef.reference.inline).isTrue()
        assertThat(channelRef.reference.model).isNotNull
        assertThat(messageInline.message.payload).isInstanceOf(SchemaInterface.SchemaInline::class.java)

        val components = bundled.components as ComponentInterface.ComponentInline
        assertThat(components.component.schemas).isNull()
    }

    private fun expectedSingleFileBundled(file: File): AsyncApiDocument {
        return bundlerFixtures.validatedDocument(file)
    }
}
