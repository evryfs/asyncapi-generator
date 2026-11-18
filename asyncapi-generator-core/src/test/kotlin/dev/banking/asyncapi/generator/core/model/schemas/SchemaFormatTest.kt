package dev.banking.asyncapi.generator.core.model.schemas

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaFormatTest {
    @Test
    fun `fromValue recognizes asyncapi schema object formats`() {
        assertEquals(
            SchemaFormat.ASYNCAPI_3_0_0,
            SchemaFormat.fromValue("application/vnd.aai.asyncapi;version=3.0.0"),
        )
        assertEquals(
            SchemaFormat.ASYNCAPI_3_0_0_JSON,
            SchemaFormat.fromValue("application/vnd.aai.asyncapi+json;version=3.0.0"),
        )
        assertEquals(
            SchemaFormat.ASYNCAPI_3_0_0_YAML,
            SchemaFormat.fromValue("application/vnd.aai.asyncapi+yaml;version=3.0.0"),
        )
        assertTrue(SchemaFormat.ASYNCAPI_3_0_0_YAML.isAsyncApiSchemaObject)
    }

    @Test
    fun `fromValue recognizes native avro formats`() {
        assertEquals(
            SchemaFormat.AVRO_1_9_0,
            SchemaFormat.fromValue("application/vnd.apache.avro;version=1.9.0"),
        )
        assertEquals(
            SchemaFormat.AVRO_1_9_0_JSON,
            SchemaFormat.fromValue("application/vnd.apache.avro+json;version=1.9.0"),
        )
        assertEquals(
            SchemaFormat.AVRO_1_9_0_YAML,
            SchemaFormat.fromValue("application/vnd.apache.avro+yaml;version=1.9.0"),
        )
        assertTrue(SchemaFormat.AVRO_1_9_0_JSON.isNativeAvro)
    }

    @Test
    fun `fromValue recognizes native protobuf formats`() {
        assertEquals(
            SchemaFormat.PROTOBUF_2,
            SchemaFormat.fromValue("application/vnd.google.protobuf;version=2"),
        )
        assertEquals(
            SchemaFormat.PROTOBUF_3,
            SchemaFormat.fromValue("application/vnd.google.protobuf;version=3"),
        )
        assertTrue(SchemaFormat.PROTOBUF_3.isNativeProtobuf)
    }

    @Test
    fun `fromValue recognizes JSON Schema Draft 07 formats`() {
        assertEquals(
            SchemaFormat.JSON_SCHEMA_DRAFT_07_JSON,
            SchemaFormat.fromValue("application/schema+json;version=draft-07"),
        )
        assertEquals(
            SchemaFormat.JSON_SCHEMA_DRAFT_07_YAML,
            SchemaFormat.fromValue("application/schema+yaml;version=draft-07"),
        )
        assertTrue(SchemaFormat.JSON_SCHEMA_DRAFT_07_JSON.isJsonSchemaDraft07)
        assertTrue(SchemaFormat.JSON_SCHEMA_DRAFT_07_YAML.isJsonSchemaDraft07)
    }

    @Test
    fun `fromValue returns null for unknown schema format`() {
        assertNull(SchemaFormat.fromValue("application/unknown"))
    }
}
