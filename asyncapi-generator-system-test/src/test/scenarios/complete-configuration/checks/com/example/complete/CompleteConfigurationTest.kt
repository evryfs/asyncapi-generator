package com.example.complete

import com.example.codegen.GeneratedPayload
import com.example.complete.avro.AccountSnapshot as AvroAccountSnapshot
import com.example.complete.kotlin.client.consumer.AccountEventsConsumer
import com.example.complete.kotlin.client.producer.AccountEventsProducer
import com.example.complete.kotlin.model.AccountUpdatedV1Payload as KotlinPayload
import com.example.complete.java.model.AccountUpdatedV1Payload as JavaPayload
import com.example.complete.protobuf.AccountSnapshot as ProtobufAccountSnapshot
import com.example.complete.protobuf.accountSnapshot
import org.apache.avro.Schema
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import org.springframework.messaging.handler.annotation.Header
import org.springframework.validation.annotation.Validated
import systemtest.support.Classpath
import systemtest.support.Documents
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Every generator profile in one project: models, clients, native formats, schemas, and documents. */
class CompleteConfigurationTest {

    @Test
    fun `the configured model annotation is applied to Kotlin and Java models`() {
        assertTrue(KotlinPayload::class.java.isAnnotationPresent(GeneratedPayload::class.java))
        assertTrue(JavaPayload::class.java.isAnnotationPresent(GeneratedPayload::class.java))
    }

    @Test
    fun `the java-record model type generates a record with every property`() {
        assertTrue(JavaPayload::class.java.isRecord)
        assertEquals(listOf("accountId", "displayName"), JavaPayload::class.java.recordComponents.map { it.name })
        assertEquals("acct-1", JavaPayload("acct-1", null).accountId())
    }

    @Test
    fun `the producer sends the payload with its key and required header`() {
        val payload = KotlinPayload(accountId = "acct-1")
        val send = object : AccountEventsProducer {}
            .sendAccountUpdatedV1(payload, messageKey = "acct-1", xExampleCorrelationId = "corr-1")
        assertTrue(send.isCompletedExceptionally, "default producer methods fail until implemented")

        val header = AccountEventsProducer::class.java.methods
            .single { it.name == "sendAccountUpdatedV1" }
            .parameters.firstNotNullOf { it.getAnnotation(Header::class.java) }
        assertEquals("X-EXAMPLE-CORRELATION-ID", header.name)
        assertTrue(header.required)
    }

    @Test
    fun `client contracts are validated and resolve the topic from a Spring property`() {
        listOf(AccountEventsProducer::class.java, AccountEventsConsumer::class.java).forEach { contract ->
            assertTrue(contract.isAnnotationPresent(Validated::class.java), "$contract is @Validated")
        }
        assertEquals("account.\${kafka.environment}.events.v1", AccountEventsProducer.ACCOUNT_EVENTS_TOPIC_ADDRESS)
    }

    @Test
    fun `the Avro SpecificRecord round-trips through binary encoding`() {
        val record = AvroAccountSnapshot.newBuilder().setAccountId("acct-1").setDisplayName("Ada").build()
        val bytes = ByteArrayOutputStream().use { output ->
            val encoder = EncoderFactory.get().binaryEncoder(output, null)
            SpecificDatumWriter(AvroAccountSnapshot::class.java).write(record, encoder)
            encoder.flush()
            output.toByteArray()
        }
        val decoded = SpecificDatumReader(AvroAccountSnapshot::class.java)
            .read(null, DecoderFactory.get().binaryDecoder(bytes, null))
        assertEquals(record, decoded)
    }

    @Test
    fun `the native Avro schema is on the classpath and matches the SpecificRecord`() {
        val schema = Schema.Parser().parse(Classpath.resource("com/example/complete/avro/AccountSnapshot.avsc"))
        assertEquals(AvroAccountSnapshot.getClassSchema(), schema)
    }

    @Test
    fun `the Protobuf message built with the Kotlin DSL round-trips through bytes`() {
        val message = accountSnapshot {
            accountId = "acct-1"
            displayName = "Ada"
        }
        val decoded = ProtobufAccountSnapshot.parseFrom(message.toByteArray())
        assertEquals(message, decoded)
        assertTrue(decoded.hasDisplayName(), "optional field presence survives")
        assertTrue(Classpath.hasResource("com/example/complete/protobuf/AccountSnapshot.proto"))
    }

    @Test
    fun `schema-only profiles write schemas to the classpath and generate no classes`() {
        val avro = Schema.Parser().parse(Classpath.resource("com/example/complete/schema/avro/AccountUpdatedV1Payload.avsc"))
        assertEquals(listOf("accountId", "displayName"), avro.fields.map { it.name() })
        assertFalse(Classpath.hasClass("com.example.complete.schema.avro.AccountUpdatedV1Payload"))

        val proto = Classpath.resource("com/example/complete/schema/protobuf/AccountSnapshot.proto")
        assertTrue("message AccountSnapshot" in proto)
        assertFalse(Classpath.hasClass("com.example.complete.schema.protobuf.AccountSnapshot"))

        val json = Documents.parse(
            Classpath.resource("com/example/complete/schema/json/AccountUpdatedV1Payload.schema.json"),
            "json",
        )
        assertEquals("http://json-schema.org/draft-07/schema#", json.path("\$schema").asText())
        assertEquals(listOf("accountId"), json.path("required").map { it.asText() })
        assertFalse(Classpath.hasClass("com.example.complete.schema.json.AccountUpdatedV1Payload"))
    }

    @Test
    fun `the YAML and JSON document profiles describe the same bundled document`() {
        val yaml = Documents.read("documents/asyncapi.yaml")
        assertEquals("3.0.0", yaml.path("asyncapi").asText())
        assertEquals(yaml, Documents.read("documents/asyncapi.json"))
    }

    @Test
    fun `an outputFile next to code generation writes the same bundled document`() {
        assertEquals(Documents.read("documents/asyncapi.yaml"), Documents.read("bundles/asyncapi.yaml"))
    }
}
