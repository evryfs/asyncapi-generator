package com.example.inlineunions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertIs
import com.example.inlineunions.java.Box as JavaBox
import com.example.inlineunions.java.Delivery as JavaDelivery
import com.example.inlineunions.java.PickupPoint as JavaPickupPoint
import com.example.inlineunions.kotlin.Box as KotlinBox
import com.example.inlineunions.kotlin.Delivery as KotlinDelivery
import com.example.inlineunions.kotlin.Destination as KotlinDestination
import com.example.inlineunions.kotlin.Parcel as KotlinParcel
import com.example.inlineunions.kotlin.PickupPoint as KotlinPickupPoint

/**
 * A property whose schema is an inline oneOf or anyOf gets a union type of its own, named after
 * its title or the property, and its members implement it.
 */
class InlineUnionsTest {
    private val kotlinJson = jacksonObjectMapper()
    private val javaJson = ObjectMapper()
    private val delivery = """{"destination":{"pointId":"P1"},"shipment":{"weight":2.5}}"""

    @Test
    fun `Kotlin inline union properties read their members`() {
        val read = kotlinJson.readValue<KotlinDelivery>(delivery)

        assertIs<KotlinPickupPoint>(read.destination as KotlinDestination)
        assertIs<KotlinBox>(read.shipment as KotlinParcel)
    }

    @Test
    fun `Java inline union properties read their members`() {
        val read = javaJson.readValue(delivery, JavaDelivery::class.java)

        assertIs<JavaPickupPoint>(read.getDestination())
        assertIs<JavaBox>(read.getShipment())
    }
}
