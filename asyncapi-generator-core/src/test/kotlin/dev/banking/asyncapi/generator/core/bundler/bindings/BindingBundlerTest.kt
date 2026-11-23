package dev.banking.asyncapi.generator.core.bundler.bindings

import dev.banking.asyncapi.generator.core.bundler.BundlingRun
import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BindingBundlerTest {
    private val walk = BundlingWalk()

    @Test
    fun `bundles an external Kafka key schema in an inline protocol binding`() {
        val binding =
            kafkaBinding(
                content =
                    mapOf(
                        "key" to mapOf("\$ref" to "key-schemas.yaml#/AccountKey"),
                        "bindingVersion" to "0.5.0",
                    ),
            )

        val bundled =
            assertIs<BindingInterface.BindingInline>(
                walk.member(BindingInterface.BindingInline(binding), walk.binding, BundlingRun().rootScope()),
            ).binding

        val originalKey = assertIs<SchemaInterface.SchemaReference>(binding.kafkaKeySchema)
        val bundledKey = assertIs<SchemaInterface.SchemaReference>(bundled.content["key"])
        assertEquals("#/components/schemas/AccountKey", bundledKey.reference.ref, "the key keeps its source name")
        assertSame(bundledKey, bundled.kafkaKeySchema)
        assertSame(bundledKey, bundled.protocolBindings.single().schemaFields["key"])
        assertEquals(
            mapOf("\$ref" to "key-schemas.yaml#/AccountKey"),
            assertIs<Map<*, *>>(bundled.protocolBindings.single().content)["key"],
        )
    }

    @Test
    fun `bundles an external Kafka key schema in a referenced component binding`() {
        val binding =
            kafkaBinding(
                content =
                    mapOf(
                        "kafka" to
                            mapOf(
                                "key" to mapOf("\$ref" to "key-schemas.yaml#/AccountKey"),
                                "bindingVersion" to "0.5.0",
                            ),
                    ),
            )
        val reference = Reference("bindings.yaml#/AccountBinding", model = binding)
        val bindingReference = BindingInterface.BindingReference(reference)

        val bundled =
            assertIs<BindingInterface.BindingReference>(
                walk.member(bindingReference, walk.binding, BundlingRun().rootScope()),
            )

        assertTrue(bundled.reference.inline)
        assertFalse(reference.inline, "the input reference is not changed")
        assertSame(binding, reference.model, "the input reference model is not changed")
        val bundledBinding = assertIs<Binding>(bundled.reference.model)
        val originalKey = assertIs<SchemaInterface.SchemaReference>(binding.kafkaKeySchema)
        val bundledKafka = assertIs<Map<*, *>>(bundledBinding.content["kafka"])
        val bundledKey = assertIs<SchemaInterface.SchemaReference>(bundledKafka["key"])
        assertEquals("#/components/schemas/AccountKey", bundledKey.reference.ref, "the key keeps its source name")
        assertSame(bundledKey, bundledBinding.kafkaKeySchema)
    }

    @Test
    fun `bundles external Kafka operation schema fields`() {
        val groupId = schemaReference("group-id.yaml#/GroupId")
        val clientId = schemaReference("client-id.yaml#/ClientId")
        val binding =
            Binding(
                content =
                    mapOf(
                        "groupId" to mapOf("\$ref" to "group-id.yaml#/GroupId"),
                        "clientId" to mapOf("\$ref" to "client-id.yaml#/ClientId"),
                        "bindingVersion" to "0.5.0",
                    ),
                protocolBindings =
                    listOf(
                        ProtocolBinding(
                            protocol = "kafka",
                            location = BindingLocation.OPERATION,
                            content = emptyMap<String, Any?>(),
                            bindingVersion = "0.5.0",
                            schemaFields =
                                mapOf(
                                    "groupId" to groupId,
                                    "clientId" to clientId,
                                ),
                        ),
                    ),
            )

        val bundled =
            assertIs<BindingInterface.BindingInline>(
                walk.member(BindingInterface.BindingInline(binding), walk.binding, BundlingRun().rootScope()),
            ).binding

        assertEquals("string", assertIs<SchemaInterface.SchemaInline>(bundled.content["groupId"]).schema.type)
        assertEquals("string", assertIs<SchemaInterface.SchemaInline>(bundled.content["clientId"]).schema.type)
        assertSame(bundled.content["groupId"], bundled.protocolBindings.single().schemaFields["groupId"])
        assertSame(bundled.content["clientId"], bundled.protocolBindings.single().schemaFields["clientId"])
    }

    @Test
    fun `preserves already self-contained Kafka operation schema fields`() {
        val binding =
            Binding(
                content =
                    mapOf(
                        "groupId" to true,
                        "clientId" to mapOf("type" to "string"),
                    ),
                protocolBindings =
                    listOf(
                        ProtocolBinding(
                            protocol = "kafka",
                            location = BindingLocation.OPERATION,
                            content = emptyMap<String, Any?>(),
                            bindingVersion = null,
                            schemaFields =
                                mapOf(
                                    "groupId" to SchemaInterface.BooleanSchema(true),
                                    "clientId" to SchemaInterface.SchemaInline(Schema(type = "string")),
                                ),
                        ),
                    ),
            )

        val bundled =
            assertIs<BindingInterface.BindingInline>(
                walk.member(BindingInterface.BindingInline(binding), walk.binding, BundlingRun().rootScope()),
            ).binding

        assertEquals(binding, bundled)
    }

    private fun kafkaBinding(content: Map<String, Any?>): Binding {
        val key = schemaReference("key-schemas.yaml#/AccountKey", type = "object")
        return Binding(
            content = content,
            protocolBindings =
                listOf(
                    ProtocolBinding(
                        protocol = "kafka",
                        location = BindingLocation.MESSAGE,
                        content = (content["kafka"] as? Map<*, *>) ?: content,
                        bindingVersion = "0.5.0",
                        schemaFields = mapOf("key" to key),
                    ),
                ),
        )
    }

    private fun schemaReference(
        ref: String,
        type: String = "string",
    ) = SchemaInterface.SchemaReference(
        Reference(
            ref = ref,
            model = Schema(type = type),
        ),
    )
}
