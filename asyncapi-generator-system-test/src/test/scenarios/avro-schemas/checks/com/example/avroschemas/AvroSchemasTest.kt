package com.example.avroschemas

import systemtest.support.Classpath
import org.apache.avro.Schema
import kotlin.test.Test
import kotlin.test.assertEquals

/** Generated `.avsc` files are valid Avro and keep the contract's types, names and defaults. */
class AvroSchemasTest {
    private val schemas: Map<String, Schema> = parse(listOf("Tier", "AuditMetadata", "AccountChanged"))

    @Test
    fun `every generated schema parses with Avro`() {
        assertEquals(setOf("Tier", "AuditMetadata", "AccountChanged"), schemas.keys)
    }

    @Test
    fun `descriptions survive JSON escaping`() {
        assertEquals("A description with \"quotes\", a backslash \\ and a line break.", schemas.getValue("AccountChanged").doc)
    }

    @Test
    fun `nested collections keep their element types`() {
        val account = schemas.getValue("AccountChanged")
        val limits = account.getField("limits").schema().types.last()
        assertEquals(Schema.Type.LONG, limits.valueType.valueType.type)
        val tags = account.getField("tags").schema().types.last()
        assertEquals(Schema.Type.STRING, tags.elementType.elementType.type)
        assertEquals("com.example.avroschemas.AuditMetadata", account.getField("audit").schema().types.last().fullName)
        assertEquals(Schema.Type.STRING, account.getField("labels").schema().types.last().elementType.type)
        assertEquals(Schema.Type.STRING, account.getField("reference").schema().types.last().type)
    }

    @Test
    fun `contract defaults become Avro defaults`() {
        val account = schemas.getValue("AccountChanged")
        assertEquals("silver", account.getField("tier").defaultVal())
        assertEquals("say \"hi\"", account.getField("note").defaultVal())
        assertEquals(3, account.getField("retries").defaultVal())
        assertEquals(org.apache.avro.JsonProperties.NULL_VALUE, account.getField("audit").defaultVal())
    }

    /** Parses with one parser, in dependency order, so references resolve as a registry would. */
    private fun parse(names: List<String>): Map<String, Schema> {
        val parser = Schema.Parser()
        return names.associateWith { name -> parser.parse(Classpath.resource("com/example/avroschemas/$name.avsc")) }
    }
}
