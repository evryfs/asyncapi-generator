package com.example.avrotypes

import org.apache.avro.Schema
import systemtest.support.Classpath
import kotlin.test.Test
import kotlin.test.assertEquals

/** Generated `.avsc` files use the wire width the models use and resolve every schema they refer to. */
class AvroSchemaTypesTest {
    private val metrics: Schema = parse(listOf("Cat", "Dog", "Metrics")).getValue("Metrics")

    @Test
    fun `integers beyond 32 bits are long`() {
        assertEquals(Schema.Type.LONG, metrics.getField("counter").schema().type)
        assertEquals(Schema.Type.INT, metrics.getField("small").schema().types.last().type)
    }

    @Test
    fun `an anyOf union is inlined as an Avro union of its members`() {
        val members = metrics.getField("pet").schema().types.map { it.fullName }
        assertEquals(listOf("null", "com.example.avrotypes.Cat", "com.example.avrotypes.Dog"), members)
    }

    /** Parses with one parser, in dependency order, so references resolve as a registry would. */
    private fun parse(names: List<String>): Map<String, Schema> {
        val parser = Schema.Parser()
        return names.associateWith { name -> parser.parse(Classpath.resource("com/example/avrotypes/$name.avsc")) }
    }
}
