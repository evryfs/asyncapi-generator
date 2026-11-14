package com.example.shared

import com.example.models.SomethingUpdatedEvent as ModelOnlyEvent
import com.example.shared.client.consumer.SomethingUpdatedConsumer
import com.example.shared.client.producer.SomethingUpdatedProducer
import com.example.shared.model.SomethingUpdatedEvent
import systemtest.support.Classpath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Client configuration shared by every execution only shapes the executions that generate a client. */
class SharedClientConfigTest {

    @Test
    fun `the client execution generates its producer and consumer`() {
        val event = SomethingUpdatedEvent(id = "id-1")
        object : SomethingUpdatedProducer {}.sendSomethingUpdated(event)
        object : SomethingUpdatedConsumer {}.listenSomethingUpdated(event, "topic")
    }

    @Test
    fun `executions without a client package still generate their models and schemas`() {
        assertEquals("id-1", ModelOnlyEvent(id = "id-1").id)
        assertTrue(Classpath.hasResource("com/example/schemas/SomethingUpdatedEvent.avsc"))
    }
}
