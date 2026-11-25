package com.example.unionmembers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertIs
import com.example.unionmembers.java.Cat as JavaCat
import com.example.unionmembers.java.Dog as JavaDog
import com.example.unionmembers.java.Pet as JavaPet
import com.example.unionmembers.java.PetMember2 as JavaPetMember2
import com.example.unionmembers.kotlin.Cat as KotlinCat
import com.example.unionmembers.kotlin.Dog as KotlinDog
import com.example.unionmembers.kotlin.Pet as KotlinPet
import com.example.unionmembers.kotlin.PetMember2 as KotlinPetMember2

/**
 * An inline object member of a union becomes a model of its own, named after its title or its
 * position in the union, so the union reads it.
 */
class InlineUnionMembersTest {
    private val dog = """{"bark":"woof"}"""
    private val untitled = """{"purr":true}"""
    private val cat = """{"meow":"mew"}"""

    @Test
    fun `Kotlin unions read their inline members`() {
        val json = jacksonObjectMapper()

        assertIs<KotlinDog>(json.readValue<KotlinPet>(dog))
        assertIs<KotlinPetMember2>(json.readValue<KotlinPet>(untitled))
        assertIs<KotlinCat>(json.readValue<KotlinPet>(cat))
    }

    @Test
    fun `Java unions read their inline members`() {
        val json = ObjectMapper()

        assertIs<JavaDog>(json.readValue(dog, JavaPet::class.java))
        assertIs<JavaPetMember2>(json.readValue(untitled, JavaPet::class.java))
        assertIs<JavaCat>(json.readValue(cat, JavaPet::class.java))
    }
}
