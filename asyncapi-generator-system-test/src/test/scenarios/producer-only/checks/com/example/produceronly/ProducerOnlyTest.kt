package com.example.produceronly

import com.example.producer.client.producer.UserSignedUpProducer
import com.example.producer.model.User
import systemtest.support.Classpath
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Disabling consumer generation keeps the producers and drops every consumer. */
class ProducerOnlyTest {

    @Test
    fun `producers are generated`() {
        val send = object : UserSignedUpProducer {}.sendUserSignedUp(User(id = java.util.UUID.randomUUID(), username = "ada"))
        assertTrue(send.isCompletedExceptionally)
    }

    @Test
    fun `no consumer is generated`() {
        listOf("UserSignedUpConsumer", "UserLoggedOutConsumer").forEach { name ->
            assertFalse(Classpath.hasClass("com.example.producer.client.consumer.$name"), "$name is not generated")
        }
    }
}
