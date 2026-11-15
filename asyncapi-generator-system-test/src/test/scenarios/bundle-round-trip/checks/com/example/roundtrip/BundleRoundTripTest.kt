package com.example.roundtrip

import com.fasterxml.jackson.databind.JsonNode
import systemtest.support.Documents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The bundle is a self-contained document that keeps what the contract says. The system test
 * additionally generates code from the bundle again and requires it to match the original code.
 */
class BundleRoundTripTest {
    private val contract = Documents.read("bundles/contract.yaml")
    private val tuple = Documents.read("bundles/tuple.yaml")

    @Test
    fun `bundles contain no external references`() {
        assertEquals(emptyList(), Documents.externalReferences(contract))
        assertEquals(emptyList(), Documents.externalReferences(tuple))
    }

    @Test
    fun `the reply channel and messages stay references to root channels`() {
        val reply = contract.at("/operations/placeOrder/reply")
        assertEquals("#/channels/orderReplies", reply.path("channel").path("\$ref").asText())
        assertEquals(
            listOf("#/channels/orderReplies/messages/orderAccepted"),
            reply.path("messages").map { it.path("\$ref").asText() },
        )
        assertFalse(contract.at("/channels/orderReplies/messages/orderAccepted").isMissingNode)
    }

    @Test
    fun `references through components to an external file are inlined`() {
        val region = contract.at("/channels/orders/parameters/region")
        assertEquals("Region the order was placed in", region.path("description").asText())
        assertEquals(listOf("eu", "us"), region.path("enum").map { it.asText() })
        assertEquals("orders", contract.at("/channels/orders/tags/0/name").asText())
    }

    @Test
    fun `a local reference in an external Kafka key binding points into the bundle`() {
        val key = contract.at("/channels/orders/messages/orderPlaced/bindings/kafka/key")
        assertEquals("#/components/schemas/OrderKey", key.path("\$ref").asText())
        assertEquals("string", contract.at("/components/schemas/OrderKey/type").asText())
    }

    @Test
    fun `a deep pointer into an external component schema is not named after the component`() {
        val total = contract.at("/components/schemas/OrderPlacedPayload/properties/total")
        assertEquals("number", total.path("type").asText())
        assertEquals(setOf("total", "lines"), contract.at("/components/schemas/Order/properties").propertyNames())
    }

    @Test
    fun `tuple items with an external reference are inlined`() {
        val items = tuple.at("/channels/quotes/messages/quoted/payload/items")
        assertTrue(items.isArray, "Tuple items stay an array")
        assertEquals(setOf("amount", "currency"), items.get(0).path("properties").propertyNames())
        assertEquals("string", items.get(1).path("type").asText())
    }

    @Test
    fun `same-named files in different directories generate separate schemas`() {
        assertTrue(fieldsOf("com.example.roundtrip.model.LeftInner").containsAll(listOf("leftValue", "next")))
        assertTrue(fieldsOf("com.example.roundtrip.model.RightInner").containsAll(listOf("rightValue", "code")))
    }

    @Test
    fun `specification extensions are kept on the objects that carry them`() {
        assertEquals("payments-team", contract.path("x-contract-owner").asText())
        assertEquals("stable", contract.at("/info/x-lifecycle").asText())
        assertEquals("eu", contract.at("/servers/broker/x-server-region").asText())
        assertEquals("7d", contract.at("/channels/orders/x-channel-retention").asText())
        assertEquals("checkout", contract.at("/operations/placeOrder/x-operation-owner").asText())
        assertEquals("30s", contract.at("/operations/placeOrder/reply/x-reply-timeout").asText())
        assertEquals("shared building blocks", contract.at("/components/x-components-note").asText())
        assertEquals("region-value", contract.at("/channels/orders/parameters/region/x-parameter-note").asText())
        assertEquals("tag-value", contract.at("/channels/orders/tags/0/x-tag-note").asText())
        assertEquals("message-value", contract.at("/channels/orders/messages/orderPlaced/x-message-note").asText())
    }

    private fun JsonNode.propertyNames(): Set<String> = properties().map { it.key }.toSet()

    private fun fieldsOf(className: String): List<String> =
        Class.forName(className).declaredFields.map { it.name }.filterNot { it.startsWith("$") }
}
