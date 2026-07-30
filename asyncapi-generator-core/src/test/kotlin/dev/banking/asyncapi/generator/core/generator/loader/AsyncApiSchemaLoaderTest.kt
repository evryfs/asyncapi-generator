package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaFormat
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AsyncApiSchemaLoaderTest {

    @Test
    fun `should load explicit schemas`() {
        val components = Component(
            schemas = mapOf(
                "User" to SchemaInterface.SchemaInline(Schema(type = "object"))
            )
        )
        val doc = docWithComponents(components)
        val loaded = AsyncApiSchemaLoader.load(doc)
        assertTrue(loaded.containsKey("User"))
    }

    @Test
    fun `should harvest schemas from message payloads`() {
        val components = Component(
            messages = mapOf(
                "UserSignedUp" to MessageInterface.MessageInline(
                    Message(
                        payload = SchemaInterface.SchemaInline(Schema(type = "object"))
                    )
                )
            )
        )
        val doc = docWithComponents(components)
        val loaded = AsyncApiSchemaLoader.load(doc)
        assertTrue(loaded.containsKey("UserSignedUpPayload"))
    }

    @Test
    fun `should harvest referenced payload schemas from referenced channel messages`() {
        val payloadSchema = Schema(type = "object")
        val message =
            Message(
                name = "BillingAccountCreatedV1",
                payload =
                    SchemaInterface.SchemaReference(
                        Reference(
                            ref = "#/components/schemas/BillingAccountCreatedV1Payload",
                            model = payloadSchema,
                        ),
                    ),
            )
        val document =
            AsyncApiDocument(
                asyncapi = "3.0.0",
                info = dev.banking.asyncapi.generator.core.model.info.Info("T", "1"),
                channels =
                    mapOf(
                        "billingAccountCreatedV1" to
                            ChannelInterface.ChannelInline(
                                Channel(
                                    messages =
                                        mapOf(
                                            "billingAccountCreatedV1" to
                                                MessageInterface.MessageReference(
                                                    Reference(
                                                        ref =
                                                            "./messages.yaml#/components/messages/" +
                                                                "BillingAccountCreatedV1",
                                                        model = message,
                                                    ),
                                                ),
                                        ),
                                ),
                            ),
                    ),
            )

        val loaded = AsyncApiSchemaLoader.load(document)

        assertSame(payloadSchema, loaded["BillingAccountCreatedV1Payload"])
    }

    @Test
    fun `should load referenced component schemas using the component name`() {
        val payloadSchema = Schema(type = "object")
        val components =
            Component(
                schemas =
                    mapOf(
                        "AccountPayload" to
                            SchemaInterface.SchemaReference(
                                Reference(
                                    ref = "./schemas.yaml#/components/schemas/ExternalAccount",
                                    model = payloadSchema,
                                ),
                            ),
                    ),
            )

        val loaded = AsyncApiSchemaLoader.load(docWithComponents(components))

        assertSame(payloadSchema, loaded["AccountPayload"])
        assertFalse(loaded.containsKey("ExternalAccount"))
    }

    @Test
    fun `should stop schema usage traversal at a previously visited recursive reference`() {
        val recursiveReference = Reference(ref = "#/components/schemas/RecursiveNodePayload")
        val recursiveSchema =
            Schema(
                type = "object",
                properties =
                    mapOf(
                        "children" to
                            SchemaInterface.SchemaInline(
                                Schema(
                                    type = "array",
                                    items = SchemaInterface.SchemaReference(recursiveReference),
                                ),
                            ),
                    ),
            )
        recursiveReference.model = recursiveSchema
        val message =
            Message(
                name = "RecursiveNode",
                payload =
                    SchemaInterface.SchemaReference(
                        Reference(
                            ref = "#/components/schemas/RecursiveNodePayload",
                            model = recursiveSchema,
                        ),
                    ),
            )
        val document =
            AsyncApiDocument(
                asyncapi = "3.0.0",
                info = dev.banking.asyncapi.generator.core.model.info.Info("T", "1"),
                channels =
                    mapOf(
                        "recursiveNodes" to
                            ChannelInterface.ChannelInline(
                                Channel(
                                    messages =
                                        mapOf(
                                            "recursiveNode" to
                                                MessageInterface.MessageInline(message),
                                        ),
                                ),
                            ),
                    ),
            )

        val loaded = AsyncApiSchemaLoader.load(document)

        assertSame(recursiveSchema, loaded["RecursiveNodePayload"])
    }

    @Test
    fun `should use the message id instead of title for inline generated schema names`() {
        val keySchema = Schema(type = "object")
        val components =
            Component(
                messages =
                    mapOf(
                        "accountUpdatedV2" to
                            MessageInterface.MessageInline(
                                Message(
                                    title = "Human-readable account update",
                                    payload = SchemaInterface.SchemaInline(Schema(type = "object")),
                                    bindings = kafkaBinding(SchemaInterface.SchemaInline(keySchema)),
                                ),
                            ),
                    ),
            )

        val loaded = AsyncApiSchemaLoader.load(docWithComponents(components))

        assertTrue(loaded.containsKey("AccountUpdatedV2Payload"))
        assertTrue(loaded.containsKey("AccountUpdatedV2Key"))
        assertFalse(loaded.containsKey("HumanReadableAccountUpdatePayload"))
        assertFalse(loaded.containsKey("HumanReadableAccountUpdateKey"))
    }

    @Test
    fun `should harvest inline object schemas from Kafka message keys`() {
        val keySchema =
            Schema(
                type = "object",
                properties =
                    mapOf(
                        "institutionId" to SchemaInterface.SchemaInline(Schema(type = "string")),
                        "accountId" to SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
            )
        val components =
            Component(
                messages =
                    mapOf(
                        "AccountUpdated" to
                            MessageInterface.MessageInline(
                                Message(
                                    name = "AccountUpdated",
                                    bindings = kafkaBinding(SchemaInterface.SchemaInline(keySchema)),
                                ),
                            ),
                    ),
            )

        val loaded = AsyncApiSchemaLoader.load(docWithComponents(components))

        assertSame(keySchema, loaded["AccountUpdatedKey"])
    }

    @Test
    fun `should harvest external referenced object schemas from Kafka message keys`() {
        val keySchema = Schema(type = "object", title = "External title is not the reference identity")
        val components =
            Component(
                messages =
                    mapOf(
                        "AccountUpdated" to
                            MessageInterface.MessageInline(
                                Message(
                                    name = "AccountUpdated",
                                    bindings =
                                        kafkaBinding(
                                            SchemaInterface.SchemaReference(
                                                Reference(
                                                    ref = "./key-schemas.yaml#/AccountKey",
                                                    model = keySchema,
                                                ),
                                            ),
                                        ),
                                ),
                            ),
                    ),
            )

        val loaded = AsyncApiSchemaLoader.load(docWithComponents(components))

        assertSame(keySchema, loaded["AccountKey"])
    }

    @Test
    fun `should load explicit multi format schemas separately from asyncapi schemas`() {
        val avroSchema = nativeAvroSchema()
        val components = Component(
            schemas = mapOf(
                "UserCreated" to SchemaInterface.MultiFormatSchemaInline(avroSchema),
            ),
        )
        val doc = docWithComponents(components)

        val loadedSchemas = AsyncApiSchemaLoader.load(doc)
        val loadedMultiFormatSchemas = AsyncApiSchemaLoader.loadMultiFormatSchemas(doc)

        assertFalse(loadedSchemas.containsKey("UserCreated"))
        assertSame(avroSchema, loadedMultiFormatSchemas["UserCreated"])
        assertEquals(SchemaFormat.AVRO_1_9_0_JSON, loadedMultiFormatSchemas["UserCreated"]?.format)
    }

    @Test
    fun `should harvest multi format schemas from message payloads`() {
        val avroSchema = nativeAvroSchema()
        val components = Component(
            messages = mapOf(
                "UserSignedUp" to MessageInterface.MessageInline(
                    Message(
                        payload = SchemaInterface.MultiFormatSchemaInline(avroSchema),
                    ),
                ),
            ),
        )
        val doc = docWithComponents(components)

        val loadedMultiFormatSchemas = AsyncApiSchemaLoader.loadMultiFormatSchemas(doc)

        assertSame(avroSchema, loadedMultiFormatSchemas["UserSignedUpPayload"])
    }

    @Test
    fun `should harvest referenced multi format payloads from referenced channel messages`() {
        val avroSchema = nativeAvroSchema()
        val message =
            Message(
                name = "UserSignedUp",
                payload =
                    SchemaInterface.SchemaReference(
                        Reference(
                            ref = "#/components/schemas/UserSignedUp",
                            model = avroSchema,
                        ),
                    ),
            )
        val document =
            AsyncApiDocument(
                asyncapi = "3.0.0",
                info = dev.banking.asyncapi.generator.core.model.info.Info("T", "1"),
                channels =
                    mapOf(
                        "userEvents" to
                            ChannelInterface.ChannelInline(
                                Channel(
                                    messages =
                                        mapOf(
                                            "userSignedUp" to
                                                MessageInterface.MessageReference(
                                                    Reference(
                                                        ref = "./messages.yaml#/components/messages/UserSignedUp",
                                                        model = message,
                                                    ),
                                                ),
                                        ),
                                ),
                            ),
                    ),
            )

        val loaded = AsyncApiSchemaLoader.loadMultiFormatSchemas(document)

        assertSame(avroSchema, loaded["UserSignedUp"])
    }

    private fun docWithComponents(component: Component): AsyncApiDocument {
        return AsyncApiDocument(
            asyncapi = "3.0.0",
            info = dev.banking.asyncapi.generator.core.model.info.Info("T", "1"),
            components = ComponentInterface.ComponentInline(component)
        )
    }

    private fun nativeAvroSchema(): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
            schema = mapOf("type" to "record", "name" to "UserCreated", "fields" to emptyList<Any>()),
        )

    private fun kafkaBinding(keySchema: SchemaInterface): Map<String, BindingInterface> =
        mapOf(
            "kafka" to
                BindingInterface.BindingInline(
                    Binding(
                        content = emptyMap(),
                        kafkaKeySchema = keySchema,
                    ),
                ),
        )
}
