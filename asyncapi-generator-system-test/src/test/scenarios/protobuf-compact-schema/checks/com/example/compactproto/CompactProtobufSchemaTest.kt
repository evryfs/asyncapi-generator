package com.example.compactproto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** A `.proto` with several declarations on one line is read like one written over many lines. */
class CompactProtobufSchemaTest {
    @Test
    fun `every top-level message of a compact schema is generated`() {
        val payload =
            EventPayload
                .newBuilder()
                .setId("event-1")
                .setNote(Note.newBuilder().setText("hello"))
                .build()

        val read = EventPayload.parseFrom(payload.toByteArray())

        assertEquals("event-1", read.id)
        assertEquals("hello", read.note.text)
    }

    @Test
    fun `the schema is written under its package`() {
        assertNotNull(javaClass.getResource("/com/example/compactproto/EventPayload.proto"))
    }
}
