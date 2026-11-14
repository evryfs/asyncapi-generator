package com.example.contract

import com.example.contract.client.consumer.AuditEventsConsumer
import com.example.contract.client.consumer.UserEventsConsumer
import com.example.contract.client.producer.AuditEventsProducer
import com.example.contract.client.producer.UserEventsProducer
import com.example.contract.model.AuditRecordedPayload
import com.example.contract.model.UserSignedUpPayload
import jakarta.validation.Valid
import org.apache.kafka.clients.producer.RecordMetadata
import org.springframework.kafka.annotation.KafkaHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SpringKafkaContractTest {
    private val userProducer = object : UserEventsProducer {}
    private val userConsumer = object : UserEventsConsumer {}

    @Test
    fun `producer exposes a send method per payload variant that fails until implemented`() {
        val sends: List<CompletableFuture<RecordMetadata>> = listOf(
            userProducer.sendUserSignedUp(UserSignedUpPayload(id = "user-1")),
            userProducer.sendUserSignedUpByteArray(byteArrayOf(1)),
            userProducer.sendUserSignedUpString("{}"),
        )

        sends.forEach { send ->
            val failure = assertFailsWith<ExecutionException> { send.get() }
            assertIs<UnsupportedOperationException>(failure.cause)
        }
    }

    @Test
    fun `consumer listen methods default to a no-op`() {
        userConsumer.listenUserSignedUp(UserSignedUpPayload(id = "user-1"), receivedTopic = "user.dev.events.v1")
    }

    @Test
    fun `topic address maps the channel parameter to a Spring property placeholder`() {
        assertEquals("user.\${kafka.environment}.events.v1", UserEventsProducer.USER_EVENTS_TOPIC_ADDRESS)
        assertEquals("user.\${kafka.environment}.events.v1", UserEventsConsumer.USER_EVENTS_TOPIC_ADDRESS)
        assertEquals("audit.events.v1", AuditEventsConsumer.AUDIT_EVENTS_TOPIC_ADDRESS)
    }

    @Test
    fun `configured validation annotations are applied to contracts and payload parameters`() {
        listOf(UserEventsProducer::class.java, UserEventsConsumer::class.java).forEach { contract ->
            assertTrue(contract.isAnnotationPresent(Validated::class.java), "$contract is @Validated")
        }
        val send = UserEventsProducer::class.java.getMethod("sendUserSignedUp", UserSignedUpPayload::class.java)
        assertTrue(send.parameters.single().isAnnotationPresent(Valid::class.java), "payload is @Valid")
    }

    @Test
    fun `contracts are plain interfaces without Spring registration`() {
        listOf(UserEventsProducer::class.java, UserEventsConsumer::class.java).forEach { contract ->
            assertTrue(contract.isInterface, "$contract is an interface")
            assertFalse(contract.isAnnotationPresent(Component::class.java), "$contract is not a @Component")
            assertFalse(contract.methods.any { it.name == "resolveTopicAddress" }, "$contract hides topic resolution")
        }
        UserEventsConsumer::class.java.methods.forEach { method ->
            assertFalse(method.isAnnotationPresent(KafkaListener::class.java), "${method.name} registers no listener")
            assertFalse(method.isAnnotationPresent(KafkaHandler::class.java), "${method.name} selects no handler")
        }
    }

    @Test
    fun `operation actions do not suppress producer or consumer generation`() {
        val auditPayload = AuditRecordedPayload(id = "audit-1")

        val failure = assertFailsWith<ExecutionException> {
            object : AuditEventsProducer {}.sendAuditRecorded(auditPayload).get()
        }
        assertIs<UnsupportedOperationException>(failure.cause)
        object : AuditEventsConsumer {}.listenAuditRecorded(auditPayload, receivedTopic = "audit.events.v1")
    }
}
