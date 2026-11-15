package com.example.payloads

import java.lang.reflect.ParameterizedType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Message payloads that are not objects get the type the models use: lists of their item type,
 * and 64-bit integers where the contract needs them, in the Kotlin and the Java client.
 */
class KafkaPayloadTypesTest {
    private val clients = mapOf("Kotlin" to "com.example.payloads.kotlin", "Java" to "com.example.payloads.java")

    @Test
    fun `array payloads are lists of the item type`() {
        clients.forEach { (language, base) ->
            listOf("OrderBatch" to "Order", "OrderList" to "Order", "Tags" to "String").forEach { (message, item) ->
                listOf("consumer.${message}Consumer" to "listen$message", "producer.${message}Producer" to "send$message")
                    .forEach { (type, method) ->
                        val payload = payloadParameter("$base.client.$type", method)
                        val list = payload.parameterizedType as ParameterizedType
                        assertEquals(List::class.java, list.rawType, "$language $type")
                        assertTrue(item in list.actualTypeArguments.single().typeName, "$language $type: $list")
                    }
            }
        }
    }

    @Test
    fun `integer payloads are Long when the contract needs 64 bits`() {
        clients.forEach { (language, base) ->
            listOf("Counter", "BigCounter").forEach { message ->
                val consumer = payloadParameter("$base.client.consumer.${message}Consumer", "listen$message")
                val producer = payloadParameter("$base.client.producer.${message}Producer", "send$message")
                assertEquals(java.lang.Long::class.java, consumer.type.kotlin.javaObjectType, "$language consumer $message")
                assertEquals(java.lang.Long::class.java, producer.type.kotlin.javaObjectType, "$language producer $message")
            }
        }
    }

    private fun payloadParameter(type: String, methodName: String) =
        Class.forName(type).methods.single { it.name == methodName }.parameters.first()
}
