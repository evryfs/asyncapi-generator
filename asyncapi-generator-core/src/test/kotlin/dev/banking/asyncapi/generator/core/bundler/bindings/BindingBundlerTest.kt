package dev.banking.asyncapi.generator.core.bundler.bindings

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class BindingBundlerTest {

    private val bundler = BindingBundler()

    @Test
    fun `bundles an external Kafka key schema in an inline protocol binding`() {
        val binding = kafkaBinding(
            content = mapOf(
                "key" to mapOf("\$ref" to "key-schemas.yaml#/AccountKey"),
                "bindingVersion" to "0.5.0",
            ),
        )

        val bundled = assertIs<BindingInterface.BindingInline>(
            bundler.bundle(BindingInterface.BindingInline(binding), BundlingContext.empty()),
        ).binding

        val originalKey = assertIs<SchemaInterface.SchemaReference>(binding.kafkaKeySchema)
        val bundledKey = assertIs<SchemaInterface.SchemaInline>(bundled.content["key"])
        assertEquals("object", bundledKey.schema.type)
        assertSame(originalKey, bundled.kafkaKeySchema)
        assertSame(originalKey, bundled.protocolBindings.single().schemaFields["key"])
        assertEquals(
            mapOf("\$ref" to "key-schemas.yaml#/AccountKey"),
            assertIs<Map<*, *>>(bundled.protocolBindings.single().content)["key"],
        )
    }

    @Test
    fun `bundles an external Kafka key schema in a referenced component binding`() {
        val binding = kafkaBinding(
            content = mapOf(
                "kafka" to mapOf(
                    "key" to mapOf("\$ref" to "key-schemas.yaml#/AccountKey"),
                    "bindingVersion" to "0.5.0",
                ),
            ),
        )
        val reference = Reference("bindings.yaml#/AccountBinding", model = binding)
        val bindingReference = BindingInterface.BindingReference(reference)

        val bundled = bundler.bundle(bindingReference, BundlingContext.empty())

        assertSame(bindingReference, bundled)
        assertTrue(reference.inline)
        val bundledBinding = assertIs<Binding>(reference.model)
        val originalKey = assertIs<SchemaInterface.SchemaReference>(binding.kafkaKeySchema)
        val bundledKafka = assertIs<Map<*, *>>(bundledBinding.content["kafka"])
        val bundledKey = assertIs<SchemaInterface.SchemaInline>(bundledKafka["key"])
        assertEquals("object", bundledKey.schema.type)
        assertSame(originalKey, bundledBinding.kafkaKeySchema)
    }

    private fun kafkaBinding(content: Map<String, Any?>): Binding {
        val key = SchemaInterface.SchemaReference(
            Reference(
                ref = "key-schemas.yaml#/AccountKey",
                model = Schema(type = "object"),
            ),
        )
        return Binding(
            content = content,
            kafkaKeySchema = key,
            protocolBindings = listOf(
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
}
