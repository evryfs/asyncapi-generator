package com.example.edgecases

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import com.example.edgecases.java.BirdPet as JavaBirdPet
import com.example.edgecases.java.BoatVehicle as JavaBoatVehicle
import com.example.edgecases.java.CarVehicle as JavaCarVehicle
import com.example.edgecases.java.CatPet as JavaCatPet
import com.example.edgecases.java.Heartbeat as JavaHeartbeat
import com.example.edgecases.java.Pet as JavaPet
import com.example.edgecases.java.Vehicle as JavaVehicle
import com.example.edgecases.kotlin.BirdPet as KotlinBirdPet
import com.example.edgecases.kotlin.BoatVehicle as KotlinBoatVehicle
import com.example.edgecases.kotlin.CarVehicle as KotlinCarVehicle
import com.example.edgecases.kotlin.CatPet as KotlinCatPet
import com.example.edgecases.kotlin.DogPet as KotlinDogPet
import com.example.edgecases.kotlin.EdgeCases as KotlinEdgeCases
import com.example.edgecases.kotlin.Heartbeat as KotlinHeartbeat
import com.example.edgecases.kotlin.Pet as KotlinPet
import com.example.edgecases.kotlin.Vehicle as KotlinVehicle

/**
 * Contract content that used to generate Kotlin or Java that did not compile or could not be read
 * back: date defaults, fieldless objects, escaped discriminators, and unions of snake_case members.
 */
class EdgeCasesTest {
    private val kotlinJson = jacksonObjectMapper()
    private val javaJson = ObjectMapper()

    @Test
    fun `Kotlin date, time, and uuid defaults hold the contract's values`() {
        val edgeCases = KotlinEdgeCases()

        assertEquals(LocalDate.of(2024, 1, 1), edgeCases.day)
        assertEquals(OffsetDateTime.parse("2024-01-01T10:15:30+01:00"), edgeCases.moment)
        assertEquals(LocalTime.of(10, 15, 30), edgeCases.time)
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), edgeCases.id)
    }

    @Test
    fun `a fieldless Kotlin object is a value that reads and writes as an empty object`() {
        assertEquals(KotlinHeartbeat(), KotlinHeartbeat())
        assertEquals(KotlinHeartbeat().hashCode(), KotlinHeartbeat().hashCode())
        assertNotEquals<Any>(KotlinHeartbeat(), "heartbeat")

        assertEquals("{}", kotlinJson.writeValueAsString(KotlinHeartbeat()))
        assertEquals(KotlinHeartbeat(), kotlinJson.readValue<KotlinHeartbeat>("{}"))
    }

    @Test
    fun `a fieldless Java object reads and writes as an empty object`() {
        assertEquals("{}", javaJson.writeValueAsString(JavaHeartbeat()))
        assertIs<JavaHeartbeat>(javaJson.readValue("{}", JavaHeartbeat::class.java))
    }

    @Test
    fun `snake_case members implement their union and are chosen by the discriminator`() {
        val cat = kotlinJson.readValue<KotlinPet>("""{"kind": "c\"at", "whiskers": 12}""")
        assertEquals(12, assertIs<KotlinCatPet>(cat).whiskers)
        assertIs<KotlinDogPet>(kotlinJson.readValue<KotlinPet>("""{"kind": "dog"}"""))
        assertIs<KotlinBirdPet>(
            kotlinJson.readValue<KotlinPet>("""{"kind": "bird_pet"}"""),
            "without a const, the schema name is the discriminator value",
        )
        assertEquals("c\"at", kotlinJson.readTree(kotlinJson.writeValueAsString(cat))["kind"].asText())
    }

    @Test
    fun `Java unions escape their discriminator values and choose the member`() {
        val cat = javaJson.readValue("""{"kind": "c\"at", "whiskers": 12}""", JavaPet::class.java)
        assertEquals(12, assertIs<JavaCatPet>(cat).whiskers)
        assertIs<JavaBirdPet>(javaJson.readValue("""{"kind": "bird_pet"}""", JavaPet::class.java))
    }

    @Test
    fun `a union without a discriminator is read as the member whose properties are present`() {
        val car = kotlinJson.readValue<KotlinVehicle>("""{"wheels": 4}""")
        assertEquals(4, assertIs<KotlinCarVehicle>(car).wheels)
        assertEquals(2, assertIs<KotlinBoatVehicle>(kotlinJson.readValue<KotlinVehicle>("""{"sails": 2}""")).sails)

        assertIs<JavaCarVehicle>(javaJson.readValue("""{"wheels": 4}""", JavaVehicle::class.java))
        assertIs<JavaBoatVehicle>(javaJson.readValue("""{"sails": 2}""", JavaVehicle::class.java))
    }

    @Test
    fun `a property typed as a union reads the member`() {
        val edgeCases = kotlinJson.readValue<KotlinEdgeCases>("""{"vehicle": {"sails": 3}, "pet": {"kind": "dog", "tail": true}}""")

        assertEquals(3, assertIs<KotlinBoatVehicle>(edgeCases.vehicle).sails)
        assertEquals(true, assertIs<KotlinDogPet>(edgeCases.pet).tail)
    }
}
