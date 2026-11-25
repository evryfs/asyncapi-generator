package com.example.headers

import org.springframework.messaging.handler.annotation.Header
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Message headers composed with `allOf` become header parameters of the client methods, with the
 * required header of each part kept, in the Kotlin and the Java client.
 */
class ComposedHeadersTest {
    private val clients = mapOf("Kotlin" to "com.example.headers.kotlin", "Java" to "com.example.headers.java")

    @Test
    fun `composed headers are method parameters`() {
        clients.forEach { (language, base) ->
            listOf(
                "$base.client.consumer.PaymentsConsumer" to "listenPaymentBooked",
                "$base.client.producer.PaymentsProducer" to "sendPaymentBooked",
            ).forEach { (type, methodName) ->
                val headers =
                    Class.forName(type).methods.single { it.name == methodName }.parameters
                        .mapNotNull { parameter -> parameter.getAnnotation(Header::class.java) }
                        .filter { header -> !header.name.startsWith("kafka_") }
                        .associate { header -> header.name to header.required }
                assertEquals(mapOf("traceId" to true, "tenant" to false), headers, "$language $type")
            }
        }
    }
}
