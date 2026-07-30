package dev.banking.asyncapi.generator.core.model.schemas

import dev.banking.asyncapi.generator.core.constants.AsyncApiConstants
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaFormatTest {

    @Test
    fun `fromValue recognizes asyncapi schema object formats`() {
        assertEquals(
            SchemaFormat.ASYNCAPI_3_0_0,
            SchemaFormat.fromValue(AsyncApiConstants.ASYNCAPI_V_3_0_0),
        )
        assertEquals(
            SchemaFormat.ASYNCAPI_3_0_0_JSON,
            SchemaFormat.fromValue(AsyncApiConstants.ASYNCAPI_V_3_0_0_JSON),
        )
        assertEquals(
            SchemaFormat.ASYNCAPI_3_0_0_YAML,
            SchemaFormat.fromValue(AsyncApiConstants.ASYNCAPI_V_3_0_0_YAML),
        )
        assertTrue(SchemaFormat.ASYNCAPI_3_0_0_YAML.isAsyncApiSchemaObject)
    }

    @Test
    fun `fromValue recognizes native avro formats`() {
        assertEquals(
            SchemaFormat.AVRO_1_9_0,
            SchemaFormat.fromValue(AsyncApiConstants.AVRO_V_1_9_0),
        )
        assertEquals(
            SchemaFormat.AVRO_1_9_0_JSON,
            SchemaFormat.fromValue(AsyncApiConstants.AVRO_V_1_9_0_JSON),
        )
        assertEquals(
            SchemaFormat.AVRO_1_9_0_YAML,
            SchemaFormat.fromValue(AsyncApiConstants.AVRO_V_1_9_0_YAML),
        )
        assertTrue(SchemaFormat.AVRO_1_9_0_JSON.isNativeAvro)
    }

    @Test
    fun `fromValue recognizes native protobuf formats`() {
        assertEquals(
            SchemaFormat.PROTOBUF_2,
            SchemaFormat.fromValue(AsyncApiConstants.PROTOBUF_V_2),
        )
        assertEquals(
            SchemaFormat.PROTOBUF_3,
            SchemaFormat.fromValue(AsyncApiConstants.PROTOBUF_V_3),
        )
        assertTrue(SchemaFormat.PROTOBUF_3.isNativeProtobuf)
    }

    @Test
    fun `fromValue recognizes JSON Schema Draft 07 formats`() {
        assertEquals(
            SchemaFormat.JSON_SCHEMA_DRAFT_07_JSON,
            SchemaFormat.fromValue(AsyncApiConstants.JSON_SCHEMA_DRAFT_07_JSON),
        )
        assertEquals(
            SchemaFormat.JSON_SCHEMA_DRAFT_07_YAML,
            SchemaFormat.fromValue(AsyncApiConstants.JSON_SCHEMA_DRAFT_07_YAML),
        )
        assertTrue(SchemaFormat.JSON_SCHEMA_DRAFT_07_JSON.isJsonSchemaDraft07)
        assertTrue(SchemaFormat.JSON_SCHEMA_DRAFT_07_YAML.isJsonSchemaDraft07)
    }

    @Test
    fun `fromValue returns null for unknown schema format`() {
        assertNull(SchemaFormat.fromValue("application/unknown"))
    }
}
