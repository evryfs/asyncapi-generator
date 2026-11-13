package com.example.javaclient

import com.example.java.client.consumer.UserLoggedOutConsumer
import com.example.java.client.consumer.UserSignedUpConsumer
import com.example.java.client.producer.UserLoggedOutProducer
import com.example.java.client.producer.UserSignedUpProducer
import com.example.java.model.User
import com.example.java.model.UserLoggedOutPayload
import com.example.java.protobuf.AccountSnapshot
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Java Spring Kafka contracts and native Protobuf messages, used from a consumer project. */
class JavaClientTest {

    @Test
    fun `every channel gets a producer and a consumer whatever its operation action`() {
        val user = User().apply {
            id = UUID.randomUUID()
            username = "ada"
        }
        val loggedOut = UserLoggedOutPayload().apply { userId = user.id }

        val sends = listOf(
            object : UserSignedUpProducer {}.sendUserSignedUp(user),
            object : UserLoggedOutProducer {}.sendUserLoggedOut(loggedOut),
        )
        sends.forEach { assertTrue(it.isCompletedExceptionally, "default producer methods fail until implemented") }

        object : UserSignedUpConsumer {}.listenUserSignedUp(user, "topic")
        object : UserLoggedOutConsumer {}.listenUserLoggedOut(loggedOut, "topic")
    }

    @Test
    fun `consumers receive the topic the record arrived on`() {
        val topic = UserLoggedOutConsumer::class.java.methods
            .single { it.name == "listenUserLoggedOut" }
            .parameters.firstNotNullOf { it.getAnnotation(Header::class.java) }
        assertEquals(KafkaHeaders.RECEIVED_TOPIC, topic.name)
    }

    @Test
    fun `the Java Protobuf message round-trips through bytes`() {
        val message = AccountSnapshot.newBuilder().setAccountId("acct-1").build()
        val decoded = AccountSnapshot.parseFrom(message.toByteArray())
        assertEquals(message, decoded)
        assertEquals(false, decoded.hasDisplayName(), "an unset optional field stays absent")
    }
}
