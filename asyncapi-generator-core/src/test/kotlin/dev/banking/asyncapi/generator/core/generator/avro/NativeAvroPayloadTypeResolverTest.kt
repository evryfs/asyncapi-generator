package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.messaging.ResolvedMessagePayload
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NativeAvroPayloadTypeResolverTest {
    private val resolver = NativeAvroPayloadTypeResolver()

    @Test
    fun `resolve returns generated type name and import from Avro namespace`() {
        val payloadType =
            resolver.resolve(
                ResolvedMessagePayload.MultiFormat(
                    typeName = "UserCreated",
                    schema =
                        MultiFormatSchema(
                            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
                            schema =
                                mapOf(
                                    "type" to "record",
                                    "name" to "UserCreated",
                                    "namespace" to "com.example.avro",
                                    "fields" to emptyList<Any>(),
                                ),
                        ),
                ),
            )

        assertEquals("UserCreated", payloadType?.typeName)
        assertEquals("com.example.avro", payloadType?.packageName)
    }

    @Test
    fun `resolve ignores non Avro messages`() {
        val payloadType =
            resolver.resolve(
                ResolvedMessagePayload.MultiFormat(
                    typeName = "UserCreated",
                    schema =
                        MultiFormatSchema(
                            schemaFormat = "application/vnd.google.protobuf;version=3",
                            schema = "message UserCreated {}",
                        ),
                ),
            )

        assertNull(payloadType)
    }

    @Test
    fun `resolve rejects native Avro schemas that do not generate named client types`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedNativeAvroPayloadType> {
                resolver.resolve(
                    ResolvedMessagePayload.MultiFormat(
                        typeName = "UserCreated",
                        schema =
                            MultiFormatSchema(
                                schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
                                schema = "string",
                            ),
                    ),
                )
            }

        assertEquals(
            """
            Native Avro payload 'UserCreated' cannot be used as a generated client type.
            The payload uses schemaFormat 'application/vnd.apache.avro+json;version=1.9.0' with Avro schema type 'STRING'.
            Generated client APIs currently require a named Avro record, enum, or fixed schema.
            """.trimIndent(),
            error.message?.trim(),
        )
    }
}
