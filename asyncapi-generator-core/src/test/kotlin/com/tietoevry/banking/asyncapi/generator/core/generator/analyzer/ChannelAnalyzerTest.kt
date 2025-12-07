package com.tietoevry.banking.asyncapi.generator.core.generator.analyzer

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import com.tietoevry.banking.asyncapi.generator.core.model.channels.Channel
import com.tietoevry.banking.asyncapi.generator.core.model.channels.ChannelInterface
import com.tietoevry.banking.asyncapi.generator.core.model.info.Info
import com.tietoevry.banking.asyncapi.generator.core.model.messages.Message
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageInterface
import com.tietoevry.banking.asyncapi.generator.core.model.operations.Operation
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationInterface
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChannelAnalyzerTest {

    private val analyzer = ChannelAnalyzer(AsyncApiContext())

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
}
