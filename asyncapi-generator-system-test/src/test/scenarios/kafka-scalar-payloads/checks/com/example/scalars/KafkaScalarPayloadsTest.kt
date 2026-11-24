package com.example.scalars

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Scalar message payloads get the type models give the same schema: dates, times and UUIDs by their
 * `format`, and numbers as doubles unless a `multipleOf` asks for decimals, in the Kotlin and the Java client.
 */
class KafkaScalarPayloadsTest {
    private val clients = mapOf("Kotlin" to "com.example.scalars.kotlin", "Java" to "com.example.scalars.java")

    @Test
    fun `scalar payloads use the model types`() {
        val expected =
            mapOf(
                "TransferId" to UUID::class.java,
                "BookedAt" to OffsetDateTime::class.java,
                "BookingDate" to LocalDate::class.java,
                "Rate" to java.lang.Double::class.java,
                "Amount" to BigDecimal::class.java,
            )
        clients.forEach { (language, base) ->
            expected.forEach { (message, type) ->
                val channel = channelOf(message)
                val consumer = payloadParameter("$base.client.consumer.${channel}Consumer", "listen$message")
                val producer = payloadParameter("$base.client.producer.${channel}Producer", "send$message")
                assertEquals(type, consumer.type.kotlin.javaObjectType, "$language consumer $message")
                assertEquals(type, producer.type.kotlin.javaObjectType, "$language producer $message")
            }
        }
    }

    private fun channelOf(message: String): String =
        when (message) {
            "TransferId" -> "TransferIds"
            "BookedAt" -> "BookingTimes"
            "BookingDate" -> "BookingDates"
            "Rate" -> "Rates"
            else -> "Amounts"
        }

    private fun payloadParameter(type: String, methodName: String) =
        Class.forName(type).methods.single { it.name == methodName }.parameters.first()
}
