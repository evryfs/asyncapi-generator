package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaFormat
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChannelAnalyzerTest {

    private val analyzer = ChannelAnalyzer()

    @Test
    fun `should default to both producer and consumer if no operations`() {
        val channel = Channel(
            messages = mapOf(
                "msg" to MessageInterface.MessageInline(
                    Message(
                        name = "MyMessage",
                        payload = SchemaInterface.SchemaInline(
                            Schema(title = "MyPayload", type = "object")
                        )
                    )
                )
            )
        )
        val doc = AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info("Title", "1.0"),
            channels = mapOf("myChannel" to ChannelInterface.ChannelInline(channel))
        )

        val result = analyzer.analyze(doc)
        val analyzed = result.channels.first()

        assertTrue(analyzed.isProducer, "Should be producer")
        assertTrue(analyzed.isConsumer, "Should be consumer")
    }

    @Test
    fun `should respect send operation as producer`() {
        val channelObj = Channel(
            messages = mapOf(
                "msg" to MessageInterface.MessageInline(
                    Message(
                        name = "MyMessage",
                        payload = SchemaInterface.SchemaInline(
                            Schema(title = "MyPayload", type = "object")
                        )
                    )
                )
            )
        )
        val channels = mapOf("myChannel" to ChannelInterface.ChannelInline(channelObj))

        val op = Operation(
            action = "send",
            channel = Reference(ref = "#/channels/myChannel", model = channelObj)
        )

        val doc = AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info("Title", "1.0"),
            channels = channels,
            operations = mapOf("myOp" to OperationInterface.OperationInline(op))
        )

        val result = analyzer.analyze(doc)
        val analyzed = result.channels.first()

        assertTrue(analyzed.isProducer, "Should be producer")
        assertEquals(false, analyzed.isConsumer, "Should NOT be consumer")
    }

    @Test
    fun `should preserve inline multi format payload separately from asyncapi messages`() {
        val avroSchema = nativeAvroSchema()
        val channel = Channel(
            messages = mapOf(
                "msg" to MessageInterface.MessageInline(
                    Message(
                        name = "MyMessage",
                        payload = SchemaInterface.MultiFormatSchemaInline(avroSchema),
                    ),
                ),
            ),
        )
        val doc = AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info("Title", "1.0"),
            channels = mapOf("myChannel" to ChannelInterface.ChannelInline(channel)),
        )

        val analyzed = analyzer.analyze(doc).channels.single()

        assertTrue(analyzed.messages.isEmpty())
        val multiFormatMessage = analyzed.multiFormatMessages.single()
        assertEquals("MyMessage", multiFormatMessage.messageName)
        assertEquals("MyMessagePayload", multiFormatMessage.payloadName)
        assertEquals(SchemaFormat.AVRO_1_9_0_JSON, multiFormatMessage.schema.format)
    }

    @Test
    fun `should preserve referenced multi format payload separately from asyncapi messages`() {
        val avroSchema = nativeAvroSchema()
        val channel = Channel(
            messages = mapOf(
                "msg" to MessageInterface.MessageInline(
                    Message(
                        name = "MyMessage",
                        payload = SchemaInterface.SchemaReference(
                            Reference(
                                ref = "#/components/schemas/UserCreated",
                                model = avroSchema,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val doc = AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info("Title", "1.0"),
            channels = mapOf("myChannel" to ChannelInterface.ChannelInline(channel)),
        )

        val analyzed = analyzer.analyze(doc).channels.single()

        assertTrue(analyzed.messages.isEmpty())
        val multiFormatMessage = analyzed.multiFormatMessages.single()
        assertEquals("MyMessage", multiFormatMessage.messageName)
        assertEquals("UserCreated", multiFormatMessage.payloadName)
        assertEquals(SchemaFormat.AVRO_1_9_0_JSON, multiFormatMessage.schema.format)
    }

    private fun nativeAvroSchema(): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
            schema = mapOf("type" to "record", "name" to "UserCreated", "fields" to emptyList<Any>()),
        )
}
