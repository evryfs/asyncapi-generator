package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SchemaNameCollision
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SchemaNameCollisionTest {

    @Test
    fun `rejects component schemas that normalize to the same name`() {
        val error =
            assertFailsWith<SchemaNameCollision> {
                AsyncApiSchemaLoader.load(
                    documentWith(
                        schemas =
                            linkedMapOf(
                                "order-item" to SchemaInterface.SchemaInline(Schema(type = "object")),
                                "order_item" to SchemaInterface.SchemaInline(Schema(type = "object")),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("OrderItem"))
        assertTrue(error.message!!.contains("components.schemas['order-item']"))
        assertTrue(error.message!!.contains("components.schemas['order_item']"))
        assertTrue(error.message!!.contains("Rename one declaration"))
        assertTrue(error.message!!.contains("share a single declaration through a reference"))
    }

    @Test
    fun `allows normalized component names that resolve to the same schema object`() {
        val sharedSchema = Schema(type = "object")

        val loaded =
            AsyncApiSchemaLoader.load(
                documentWith(
                    schemas =
                        linkedMapOf(
                            "order-item" to SchemaInterface.SchemaInline(sharedSchema),
                            "order_item" to
                                SchemaInterface.SchemaReference(
                                    Reference(
                                        ref = "#/components/schemas/order-item",
                                        model = sharedSchema,
                                    ),
                                ),
                        ),
                ),
            )

        assertSame(sharedSchema, loaded.schemaDeclarations.asyncApiSchemas["OrderItem"])
    }

    @Test
    fun `allows a component schema and payload reference to the same component declaration`() {
        val componentSchema = Schema(type = "object")
        val separatelyBundledReferenceModel = Schema(type = "object")
        assertNotSame(componentSchema, separatelyBundledReferenceModel)

        val loaded =
            AsyncApiSchemaLoader.load(
                documentWith(
                    schemas =
                        mapOf(
                            "CustomerReadPayload" to SchemaInterface.SchemaInline(componentSchema),
                        ),
                    messages =
                        mapOf(
                            "CustomerRead" to
                                inlineMessage(
                                    SchemaInterface.SchemaReference(
                                        Reference(
                                            ref = "#/components/schemas/CustomerReadPayload",
                                            model = separatelyBundledReferenceModel,
                                        ),
                                    ),
                                ),
                        ),
                ),
            )

        assertSame(componentSchema, loaded.schemaDeclarations.asyncApiSchemas["CustomerReadPayload"])
    }

    @Test
    fun `allows a component message and channel reference to the same message declaration`() {
        val componentPayload = Schema(type = "object")
        val separatelyBundledPayload = Schema(type = "object")
        val componentMessage =
            Message(
                name = "EventMessage",
                payload = SchemaInterface.SchemaInline(componentPayload),
            )
        val separatelyBundledMessage =
            Message(
                name = "EventMessage",
                payload = SchemaInterface.SchemaInline(separatelyBundledPayload),
            )
        assertNotSame(componentMessage, separatelyBundledMessage)
        assertNotSame(componentPayload, separatelyBundledPayload)

        val loaded =
            AsyncApiSchemaLoader.load(
                documentWith(
                    messages =
                        mapOf(
                            "EventMessage" to MessageInterface.MessageInline(componentMessage),
                        ),
                    channels =
                        mapOf(
                            "someEvents" to
                                ChannelInterface.ChannelInline(
                                    Channel(
                                        messages =
                                            mapOf(
                                                "EventMessage" to
                                                    MessageInterface.MessageReference(
                                                        Reference(
                                                            ref = "#/components/messages/EventMessage",
                                                            model = separatelyBundledMessage,
                                                        ),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                ),
            )

        assertSame(componentPayload, loaded.schemaDeclarations.asyncApiSchemas["EventMessagePayload"])
    }

    @Test
    fun `allows a component message alias and channel reference to the same base message`() {
        val basePayload = Schema(type = "object")
        val aliasPayload = Schema(type = "object")
        val channelPayload = Schema(type = "object")
        val baseMessage =
            Message(
                name = "SharedEvent",
                payload = SchemaInterface.SchemaInline(basePayload),
            )
        val aliasResolvedMessage =
            Message(
                name = "SharedEvent",
                payload = SchemaInterface.SchemaInline(aliasPayload),
            )
        val channelResolvedMessage =
            Message(
                name = "SharedEvent",
                payload = SchemaInterface.SchemaInline(channelPayload),
            )
        assertNotSame(baseMessage, aliasResolvedMessage)
        assertNotSame(aliasResolvedMessage, channelResolvedMessage)
        assertNotSame(basePayload, aliasPayload)
        assertNotSame(aliasPayload, channelPayload)

        val loaded =
            AsyncApiSchemaLoader.load(
                documentWith(
                    messages =
                        linkedMapOf(
                            "BaseMessage" to MessageInterface.MessageInline(baseMessage),
                            "MessageAlias" to
                                MessageInterface.MessageReference(
                                    Reference(
                                        ref = "#/components/messages/BaseMessage",
                                        model = aliasResolvedMessage,
                                    ),
                                ),
                        ),
                    channels =
                        mapOf(
                            "sharedEvents" to
                                ChannelInterface.ChannelInline(
                                    Channel(
                                        messages =
                                            mapOf(
                                                "MessageAlias" to
                                                    MessageInterface.MessageReference(
                                                        Reference(
                                                            ref = "#/components/messages/MessageAlias",
                                                            model = channelResolvedMessage,
                                                        ),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                ),
            )

        assertSame(basePayload, loaded.schemaDeclarations.asyncApiSchemas["SharedEventPayload"])
    }

    @Test
    fun `allows repeated references to the same external schema target`() {
        val firstResolvedModel = Schema(type = "object")
        val secondResolvedModel = Schema(type = "object")
        assertNotSame(firstResolvedModel, secondResolvedModel)

        val loaded =
            AsyncApiSchemaLoader.load(
                documentWith(
                    messages =
                        linkedMapOf(
                            "firstEvent" to
                                inlineMessage(
                                    SchemaInterface.SchemaReference(
                                        Reference(
                                            ref = "./schemas.yaml#/components/schemas/ExternalEventPayload",
                                            model = firstResolvedModel,
                                            sourceId = "contract.yaml",
                                        ),
                                    ),
                                ),
                            "secondEvent" to
                                inlineMessage(
                                    SchemaInterface.SchemaReference(
                                        Reference(
                                            ref = "./schemas.yaml#/components/schemas/ExternalEventPayload",
                                            model = secondResolvedModel,
                                            sourceId = "contract.yaml",
                                        ),
                                    ),
                                ),
                        ),
                ),
            )

        assertSame(firstResolvedModel, loaded.schemaDeclarations.asyncApiSchemas["ExternalEventPayload"])
    }

    @Test
    fun `rejects a component and inline message payload with the same generated name`() {
        val error =
            assertFailsWith<SchemaNameCollision> {
                AsyncApiSchemaLoader.load(
                    documentWith(
                        schemas =
                            mapOf(
                                "order-created-payload" to
                                    SchemaInterface.SchemaInline(Schema(type = "object")),
                            ),
                        messages =
                            mapOf(
                                "orderCreated" to inlineMessage(SchemaInterface.SchemaInline(Schema(type = "object"))),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("OrderCreatedPayload"))
        assertTrue(error.message!!.contains("components.schemas['order-created-payload']"))
        assertTrue(error.message!!.contains("components.messages['orderCreated'].payload"))
    }

    @Test
    fun `rejects separately constructed message payloads with the same generated name`() {
        val error =
            assertFailsWith<SchemaNameCollision> {
                AsyncApiSchemaLoader.load(
                    documentWith(
                        messages =
                            linkedMapOf(
                                "firstOrder" to
                                    inlineMessage(
                                        payload = SchemaInterface.SchemaInline(Schema(type = "object")),
                                        name = "OrderChanged",
                                    ),
                                "secondOrder" to
                                    inlineMessage(
                                        payload = SchemaInterface.SchemaInline(Schema(type = "object")),
                                        name = "OrderChanged",
                                    ),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("OrderChangedPayload"))
        assertTrue(error.message!!.contains("components.messages['firstOrder'].payload"))
        assertTrue(error.message!!.contains("components.messages['secondOrder'].payload"))
    }

    @Test
    fun `rejects external payload references with the same final fragment`() {
        val error =
            assertFailsWith<SchemaNameCollision> {
                AsyncApiSchemaLoader.load(
                    documentWith(
                        messages =
                            linkedMapOf(
                                "created" to
                                    inlineMessage(
                                        SchemaInterface.SchemaReference(
                                            Reference(
                                                ref = "./created.yaml#/OrderPayload",
                                                model = Schema(type = "object"),
                                            ),
                                        ),
                                    ),
                                "updated" to
                                    inlineMessage(
                                        SchemaInterface.SchemaReference(
                                            Reference(
                                                ref = "./updated.yaml#/OrderPayload",
                                                model = Schema(type = "object"),
                                            ),
                                        ),
                                    ),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("OrderPayload"))
        assertTrue(error.message!!.contains("./created.yaml#/OrderPayload"))
        assertTrue(error.message!!.contains("./updated.yaml#/OrderPayload"))
    }

    @Test
    fun `rejects AsyncAPI and Multi Format declarations with the same generated name`() {
        val error =
            assertFailsWith<SchemaNameCollision> {
                AsyncApiSchemaLoader.load(
                    documentWith(
                        schemas =
                            mapOf(
                                "event-payload" to SchemaInterface.SchemaInline(Schema(type = "object")),
                            ),
                        messages =
                            mapOf(
                                "event" to
                                    inlineMessage(
                                        payload = SchemaInterface.MultiFormatSchemaInline(nativeAvroSchema()),
                                    ),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("EventPayload"))
        assertTrue(error.message!!.contains("components.schemas['event-payload']"))
        assertTrue(error.message!!.contains("components.messages['event'].payload"))
    }

    @Test
    fun `rejects Boolean and Multi Format declarations with the same generated name`() {
        val error =
            assertFailsWith<SchemaNameCollision> {
                AsyncApiSchemaLoader.load(
                    documentWith(
                        schemas =
                            mapOf(
                                "event-payload" to SchemaInterface.BooleanSchema(true),
                            ),
                        messages =
                            mapOf(
                                "event" to
                                    inlineMessage(
                                        payload = SchemaInterface.MultiFormatSchemaInline(nativeAvroSchema()),
                                    ),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("EventPayload"))
        assertTrue(error.message!!.contains("components.schemas['event-payload']"))
        assertTrue(error.message!!.contains("components.messages['event'].payload"))
    }

    @Test
    fun `allows repeated use of the same Multi Format declaration`() {
        val sharedSchema = nativeAvroSchema()

        val loaded =
            AsyncApiSchemaLoader.load(
                documentWith(
                    messages =
                        linkedMapOf(
                            "firstEvent" to
                                inlineMessage(
                                    payload = SchemaInterface.MultiFormatSchemaInline(sharedSchema),
                                    name = "NativeEvent",
                                ),
                            "secondEvent" to
                                inlineMessage(
                                    payload = SchemaInterface.MultiFormatSchemaInline(sharedSchema),
                                    name = "NativeEvent",
                                ),
                        ),
                ),
            )

        assertSame(sharedSchema, loaded.schemaDeclarations.multiFormatSchemas["NativeEventPayload"])
    }

    @Test
    fun `allows same-valued Boolean declarations with the same generated name`() {
        val loaded =
            AsyncApiSchemaLoader.load(
                documentWith(
                    messages =
                        linkedMapOf(
                            "firstCheck" to
                                inlineMessage(
                                    payload = SchemaInterface.BooleanSchema(true),
                                    name = "PermissionCheck",
                                ),
                            "secondCheck" to
                                inlineMessage(
                                    payload = SchemaInterface.BooleanSchema(true),
                                    name = "PermissionCheck",
                                ),
                        ),
                ),
            )

        assertEquals(true, loaded.schemaDeclarations.booleanSchemas["PermissionCheckPayload"])
    }

    @Test
    fun `rejects differing Boolean declarations with the same generated name`() {
        val error =
            assertFailsWith<SchemaNameCollision> {
                AsyncApiSchemaLoader.load(
                    documentWith(
                        messages =
                            linkedMapOf(
                                "firstCheck" to
                                    inlineMessage(
                                        payload = SchemaInterface.BooleanSchema(true),
                                        name = "PermissionCheck",
                                    ),
                                "secondCheck" to
                                    inlineMessage(
                                        payload = SchemaInterface.BooleanSchema(false),
                                        name = "PermissionCheck",
                                    ),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("PermissionCheckPayload"))
        assertTrue(error.message!!.contains("components.messages['firstCheck'].payload"))
        assertTrue(error.message!!.contains("components.messages['secondCheck'].payload"))
    }

    @Test
    fun `rejects a Kafka key model colliding with a component source model`() {
        val error =
            assertFailsWith<SchemaNameCollision> {
                AsyncApiSchemaLoader.load(
                    documentWith(
                        schemas =
                            mapOf(
                                "account-key" to SchemaInterface.SchemaInline(Schema(type = "object")),
                            ),
                        messages =
                            mapOf(
                                "account" to
                                    kafkaKeyMessage(
                                        name = "Account",
                                        keySchema = SchemaInterface.SchemaInline(Schema(type = "object")),
                                    ),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("AccountKey"))
        assertTrue(error.message!!.contains("components.schemas['account-key']"))
        assertTrue(error.message!!.contains("components.messages['account'].bindings.kafka.key"))
    }

    @Test
    fun `rejects a Kafka key model colliding with a payload source model`() {
        val error =
            assertFailsWith<SchemaNameCollision> {
                AsyncApiSchemaLoader.load(
                    documentWith(
                        messages =
                            linkedMapOf(
                                "accountPayload" to
                                    inlineMessage(
                                        SchemaInterface.SchemaReference(
                                            Reference(
                                                ref = "./payloads.yaml#/AccountKey",
                                                model = Schema(type = "object"),
                                            ),
                                        ),
                                    ),
                                "account" to
                                    kafkaKeyMessage(
                                        name = "Account",
                                        keySchema = SchemaInterface.SchemaInline(Schema(type = "object")),
                                    ),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("AccountKey"))
        assertTrue(error.message!!.contains("components.messages['accountPayload'].payload"))
        assertTrue(error.message!!.contains("components.messages['account'].bindings.kafka.key"))
    }

    @Test
    fun `rejects distinct Kafka key source models with the same generated name`() {
        val error =
            assertFailsWith<SchemaNameCollision> {
                AsyncApiSchemaLoader.load(
                    documentWith(
                        messages =
                            linkedMapOf(
                                "firstEvent" to
                                    kafkaKeyMessage(
                                        name = "FirstEvent",
                                        keySchema =
                                            SchemaInterface.SchemaInline(
                                                Schema(type = "object", title = "Shared key"),
                                            ),
                                    ),
                                "secondEvent" to
                                    kafkaKeyMessage(
                                        name = "SecondEvent",
                                        keySchema =
                                            SchemaInterface.SchemaInline(
                                                Schema(type = "object", title = "Shared key"),
                                            ),
                                    ),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("SharedKey"))
        assertTrue(error.message!!.contains("components.messages['firstEvent'].bindings.kafka.key"))
        assertTrue(error.message!!.contains("components.messages['secondEvent'].bindings.kafka.key"))
    }

    @Test
    fun `allows a Kafka key reference to its component source model`() {
        val componentSchema = Schema(type = "object")
        val separatelyResolvedKeySchema = Schema(type = "object")
        assertNotSame(componentSchema, separatelyResolvedKeySchema)

        val loaded =
            AsyncApiSchemaLoader.load(
                documentWith(
                    schemas =
                        mapOf(
                            "AccountKey" to SchemaInterface.SchemaInline(componentSchema),
                        ),
                    messages =
                        mapOf(
                            "account" to
                                kafkaKeyMessage(
                                    name = "Account",
                                    keySchema =
                                        SchemaInterface.SchemaReference(
                                            Reference(
                                                ref = "#/components/schemas/AccountKey",
                                                model = separatelyResolvedKeySchema,
                                            ),
                                        ),
                                ),
                        ),
                ),
            )

        assertSame(componentSchema, loaded.schemas["AccountKey"])
        assertSame(componentSchema, loaded.schemaDeclarations.asyncApiSchemas["AccountKey"])
    }

    @Test
    fun `preserves declaration order for valid contracts`() {
        val loaded =
            AsyncApiSchemaLoader.load(
                documentWith(
                    schemas =
                        linkedMapOf(
                            "order-item" to SchemaInterface.SchemaInline(Schema(type = "object")),
                            "payment-method" to SchemaInterface.SchemaInline(Schema(type = "object")),
                        ),
                    messages =
                        linkedMapOf(
                            "shipmentCreated" to
                                inlineMessage(SchemaInterface.SchemaInline(Schema(type = "object"))),
                            "invoiceCreated" to
                                inlineMessage(SchemaInterface.SchemaInline(Schema(type = "object"))),
                        ),
                ),
            )

        assertEquals(
            listOf("OrderItem", "PaymentMethod", "ShipmentCreatedPayload", "InvoiceCreatedPayload"),
            loaded.schemaDeclarations.asyncApiSchemas.keys.toList(),
        )
    }

    private fun documentWith(
        schemas: Map<String, SchemaInterface> = emptyMap(),
        messages: Map<String, MessageInterface> = emptyMap(),
        channels: Map<String, ChannelInterface> = emptyMap(),
    ): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Test", version = "1.0.0"),
            channels = channels,
            components =
                ComponentInterface.ComponentInline(
                    Component(
                        schemas = schemas,
                        messages = messages,
                    ),
                ),
        )

    private fun inlineMessage(
        payload: SchemaInterface,
        name: String? = null,
    ): MessageInterface =
        MessageInterface.MessageInline(
            Message(
                name = name,
                payload = payload,
            ),
        )

    private fun kafkaKeyMessage(
        name: String,
        keySchema: SchemaInterface,
    ): MessageInterface =
        MessageInterface.MessageInline(
            Message(
                name = name,
                bindings =
                    mapOf(
                        "kafka" to
                            BindingInterface.BindingInline(
                                Binding(
                                    content = emptyMap(),
                                    kafkaKeySchema = keySchema,
                                ),
                            ),
                    ),
            ),
        )

    private fun nativeAvroSchema(): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
            schema = mapOf("type" to "record", "name" to "Event", "fields" to emptyList<Any>()),
        )
}
