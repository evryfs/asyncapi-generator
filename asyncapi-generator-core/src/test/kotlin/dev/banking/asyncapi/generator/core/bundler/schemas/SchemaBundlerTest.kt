package dev.banking.asyncapi.generator.core.bundler.schemas

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SchemaBundlerTest {

    private val bundler = SchemaBundler()

    @Test
    fun `bundle keeps an unvisited component schema reference and bundles its model`() {
        val bindingReference = Reference("#/components/schemaBindings/kafka")
        val schema = Schema(
            type = "object",
            bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
        )
        val schemaReference = Reference("#/components/schemas/User", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = bundler.bundle(schemaInterface, BundlingContext.empty())

        assertThat(bundled).isSameAs(schemaInterface)
        assertThat(schemaReference.inline).isTrue()
        assertThat(schemaReference.model).isInstanceOf(Schema::class.java)
        assertThat((schemaReference.model as Schema).bindings).containsKey("kafka")
        assertThat(bindingReference.inline).isTrue()
    }

    @Test
    fun `bundle inlines an unvisited non-component schema reference`() {
        val bindingReference = Reference("#/components/schemaBindings/kafka")
        val schema = Schema(
            type = "object",
            bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
        )
        val schemaReference = Reference("schemas.yaml#/shared/User", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = bundler.bundle(schemaInterface, BundlingContext.empty())

        assertThat(bundled).isInstanceOf(SchemaInterface.SchemaInline::class.java)
        assertThat((bundled as SchemaInterface.SchemaInline).schema.bindings).containsKey("kafka")
        assertThat(schemaReference.inline).isFalse()
        assertThat(bindingReference.inline).isTrue()
    }

    @Test
    fun `bundle does not promote a recursive root component reference`() {
        val nestedReference = Reference("#/components/schemas/Node")
        val schema = Schema(
            type = "object",
            properties = mapOf("next" to SchemaInterface.SchemaReference(nestedReference)),
        )
        nestedReference.model = schema
        val rootReference = Reference("#/components/schemas/Node", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(rootReference)
        val context = BundlingContext.empty()

        val bundled = bundler.bundle(schemaInterface, context)

        assertThat(bundled).isSameAs(schemaInterface)
        assertThat(context.schemaPromotions.schemas()).isEmpty()
    }

    @Test
    fun `bundle keeps component multi format schema references without casting to schema`() {
        val schema = nativeAvroSchema()
        val schemaReference = Reference("#/components/schemas/UserCreated", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = bundler.bundle(schemaInterface, BundlingContext.empty())

        assertThat(bundled).isSameAs(schemaInterface)
        assertThat(schemaReference.inline).isTrue()
        assertThat(schemaReference.model).isSameAs(schema)
    }

    @Test
    fun `bundle inlines non-component multi format schema references`() {
        val schema = nativeAvroSchema()
        val schemaReference = Reference("schemas.yaml#/shared/UserCreated", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = bundler.bundle(schemaInterface, BundlingContext.empty())

        assertThat(bundled).isEqualTo(SchemaInterface.MultiFormatSchemaInline(schema))
        assertThat(schemaReference.inline).isFalse()
    }

    private fun nativeAvroSchema(): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
            schema =
                mapOf(
                    "type" to "record",
                    "name" to "UserCreated",
                    "namespace" to "com.example.avro",
                    "fields" to emptyList<Any>(),
                ),
        )
}
