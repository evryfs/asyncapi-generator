package com.example.locations

import com.example.a.client.consumer.SomethingUpdatedConsumer
import com.example.a.client.producer.SomethingUpdatedProducer
import com.example.a.model.SomethingUpdatedEvent
import com.example.codegen.SampleMessage
import org.apache.avro.Schema
import systemtest.support.Classpath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Generated sources and schemas are registered with the build wherever they are written. */
class OutputLocationsTest {

    @Test
    fun `sources in a custom output directory are compiled`() {
        assertEquals("id-1", SampleMessage(id = "id-1").id)
    }

    @Test
    fun `schemas in a custom output directory reach the runtime classpath`() {
        val schema = Schema.Parser().parse(Classpath.resource("com/example/avro/SampleMessage.avsc"))
        assertEquals("SampleMessage", schema.name)
    }

    @Test
    fun `executions sharing the default output directory keep their own outputs`() {
        val event = SomethingUpdatedEvent(id = "id-1")
        object : SomethingUpdatedProducer {}.sendSomethingUpdated(event)
        object : SomethingUpdatedConsumer {}.listenSomethingUpdated(event, "topic")

        val schema = Schema.Parser().parse(Classpath.resource("com/example/b/schema/SomethingUpdatedEvent.avsc"))
        assertEquals(listOf("id"), schema.fields.map { it.name() })
        assertFalse(Classpath.hasClass("com.example.b.schema.SomethingUpdatedEvent"), "schema-only output has no class")
        assertFalse(Classpath.hasResource("com/example/a/schema"), "contract A writes no schemas")
    }
}
