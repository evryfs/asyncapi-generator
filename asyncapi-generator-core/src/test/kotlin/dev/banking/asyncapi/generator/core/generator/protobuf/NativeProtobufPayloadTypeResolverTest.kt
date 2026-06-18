package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMultiFormatMessage
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NativeProtobufPayloadTypeResolverTest {
    private val resolver = NativeProtobufPayloadTypeResolver()

    @Test
    fun `resolve returns generated type name and import from Protobuf java package`() {
        val payloadType =
            resolver.resolve(
                protobufMessage(
                    schema =
                        """
                        syntax = "proto3";

                        package banking.events;

                        option java_package = "com.example.protobuf";
                        option java_multiple_files = true;

                        message UserCreated {
                          string user_id = 1;
                        }
                        """.trimIndent(),
                ),
            )

        assertEquals("UserCreated", payloadType?.typeName)
        assertEquals("com.example.protobuf", payloadType?.packageName)
        assertEquals("com.example.protobuf.UserCreated", payloadType?.importName)
    }

    @Test
    fun `resolve falls back to Protobuf package when java package is omitted`() {
        val payloadType =
            resolver.resolve(
                protobufMessage(
                    schema =
                        """
                        syntax = "proto3";

                        package com.example.protobuf;

                        option java_multiple_files = true;

                        message UserCreated {
                          string user_id = 1;
                        }
                        """.trimIndent(),
                ),
            )

        assertEquals("UserCreated", payloadType?.typeName)
        assertEquals("com.example.protobuf", payloadType?.packageName)
        assertEquals("com.example.protobuf.UserCreated", payloadType?.importName)
    }

    @Test
    fun `resolve ignores non Protobuf messages`() {
        val payloadType =
            resolver.resolve(
                AnalyzedMultiFormatMessage(
                    messageName = "UserCreated",
                    payloadName = "UserCreated",
                    schema =
                        MultiFormatSchema(
                            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
                            schema =
                                mapOf(
                                    "type" to "record",
                                    "name" to "UserCreated",
                                    "fields" to emptyList<Any>(),
                                ),
                        ),
                ),
            )

        assertNull(payloadType)
    }

    @Test
    fun `resolve rejects schemas without Java or Protobuf package`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedNativeProtobufPayloadType> {
                resolver.resolve(
                    protobufMessage(
                        schema =
                            """
                            syntax = "proto3";

                            option java_multiple_files = true;

                            message UserCreated {
                              string user_id = 1;
                            }
                            """.trimIndent(),
                    ),
                )
            }

        assertEquals(
            """
            Native Protobuf payload 'UserCreated' cannot be used as a generated client type.
            The payload uses schemaFormat 'application/vnd.google.protobuf;version=3'.
            Reason: Protobuf client APIs require either `option java_package = "...";` or a `package ...;` declaration.
            """.trimIndent(),
            error.message?.trim(),
        )
    }

    @Test
    fun `resolve rejects schemas without java multiple files enabled`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedNativeProtobufPayloadType> {
                resolver.resolve(
                    protobufMessage(
                        schema =
                            """
                            syntax = "proto3";

                            package com.example.protobuf;

                            message UserCreated {
                              string user_id = 1;
                            }
                            """.trimIndent(),
                    ),
                )
            }

        assertEquals(
            """
            Native Protobuf payload 'UserCreated' cannot be used as a generated client type.
            The payload uses schemaFormat 'application/vnd.google.protobuf;version=3'.
            Reason: Protobuf client APIs require `option java_multiple_files = true;` so the payload message can be referenced as a top-level Java type.
            """.trimIndent(),
            error.message?.trim(),
        )
    }

    @Test
    fun `resolve rejects schemas without expected top level message`() {
        val error =
            assertFailsWith<AsyncApiGeneratorException.UnsupportedNativeProtobufPayloadType> {
                resolver.resolve(
                    protobufMessage(
                        schema =
                            """
                            syntax = "proto3";

                            package com.example.protobuf;

                            option java_multiple_files = true;

                            message AccountCreated {
                              string account_id = 1;
                            }
                            """.trimIndent(),
                    ),
                )
            }

        assertEquals(
            """
            Native Protobuf payload 'UserCreated' cannot be used as a generated client type.
            The payload uses schemaFormat 'application/vnd.google.protobuf;version=3'.
            Reason: Protobuf client APIs require a top-level message named 'UserCreated'.
            """.trimIndent(),
            error.message?.trim(),
        )
    }

    private fun protobufMessage(schema: String): AnalyzedMultiFormatMessage =
        AnalyzedMultiFormatMessage(
            messageName = "UserCreated",
            payloadName = "UserCreated",
            schema =
                MultiFormatSchema(
                    schemaFormat = "application/vnd.google.protobuf;version=3",
                    schema = schema,
                ),
        )
}
