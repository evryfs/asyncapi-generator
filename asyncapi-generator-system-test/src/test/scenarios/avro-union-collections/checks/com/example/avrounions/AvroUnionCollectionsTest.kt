package com.example.avrounions

import org.apache.avro.Schema
import systemtest.support.Classpath
import kotlin.test.Test
import kotlin.test.assertEquals

/** A union used as an array item or map value is written inline, as for a field, and every schema resolves. */
class AvroUnionCollectionsTest {
    private val shelter: Schema = parse(listOf("Cat", "Dog", "Shelter")).getValue("Shelter")
    private val members = listOf("com.example.avrounions.Dog", "com.example.avrounions.Cat")

    @Test
    fun `array items and map values hold the union members`() {
        assertEquals(members, shelter.getField("residents").schema().elementType.types.map { it.fullName })
        val byName = shelter.getField("byName").schema().types.last()
        assertEquals(members, byName.valueType.types.map { it.fullName })
    }

    @Test
    fun `an integer enum is an int`() {
        assertEquals(Schema.Type.INT, shelter.getField("level").schema().types.last().type)
    }

    /** Parses with one parser, in dependency order, so references resolve as a registry would. */
    private fun parse(names: List<String>): Map<String, Schema> {
        val parser = Schema.Parser()
        return names.associateWith { name -> parser.parse(Classpath.resource("com/example/avrounions/$name.avsc")) }
    }
}
