package com.example.discriminators

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertIs
import com.example.discriminators.java.Cat as JavaCat
import com.example.discriminators.java.Dog as JavaDog
import com.example.discriminators.java.Pet as JavaPet
import com.example.discriminators.kotlin.Cat as KotlinCat
import com.example.discriminators.kotlin.Dog as KotlinDog
import com.example.discriminators.kotlin.Pet as KotlinPet

/**
 * A union member is selected by the discriminator value on the wire, also when the member
 * fixes that value through a referenced schema that narrows a shared enum with allOf.
 */
class DiscriminatorValuesTest {
    private val kotlinJson = jacksonObjectMapper()
    private val javaJson = ObjectMapper()

    @Test
    fun `Kotlin unions select members by their wire discriminator values`() {
        assertIs<KotlinDog>(kotlinJson.readValue<KotlinPet>("""{"petType":"dog","bark":true}"""))
        assertIs<KotlinCat>(kotlinJson.readValue<KotlinPet>("""{"petType":"cat","meow":true}"""))
    }

    @Test
    fun `Java unions select members by their wire discriminator values`() {
        assertIs<JavaDog>(javaJson.readValue("""{"petType":"dog","bark":true}""", JavaPet::class.java))
        assertIs<JavaCat>(javaJson.readValue("""{"petType":"cat","meow":true}""", JavaPet::class.java))
    }
}
