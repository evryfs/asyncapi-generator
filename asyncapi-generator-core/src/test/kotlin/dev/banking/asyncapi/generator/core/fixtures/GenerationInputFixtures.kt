package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Fixture facade for generation-input tests.
 *
 * It builds focused AsyncAPI model objects for generator preparation scenarios
 * without requiring the tests to repeat the full object graph.
 */
internal class GenerationInputFixtures {
    fun generationInputWithUserSignupChannel(): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            polymorphicRelationships = emptyMap(),
            channels =
                listOf(
                    AnalyzedChannel(
                        channelName = "userEvents",
                        topic = "user.events",
                        isProducer = true,
                        isConsumer = true,
                        messages =
                            listOf(
                                AnalyzedMessage(
                                    messageName = "UserSignedUp",
                                    payloadTypeName = "UserSignedUpPayload",
                                    schema = Schema(type = "object"),
                                ),
                            ),
                    ),
                ),
        )

    fun generationInputWithObjectEnumAndPrimitive(): GenerationInput {
        val userSchema =
            Schema(
                type = "object",
                required = listOf("id"),
                properties =
                    mapOf(
                        "id" to SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
            )
        val statusSchema =
            Schema(
                type = "string",
                enum = listOf("ACTIVE", "INACTIVE"),
            )

        return GenerationInput(
            schemas =
                linkedMapOf(
                    "User" to userSchema,
                    "Status" to statusSchema,
                    "IgnoredPrimitive" to Schema(type = "string"),
                ),
            polymorphicRelationships = mapOf("User" to listOf("Command")),
            channels = emptyList(),
        )
    }

    fun generationInputWithNativeAvroSchema(): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            multiFormatSchemas =
                mapOf(
                    "UserCreated" to nativeAvroUserCreatedSchema(namespace = "com.example.avro"),
                ),
            polymorphicRelationships = emptyMap(),
            channels = emptyList(),
        )

    fun generationInputWithNativeProtobufSchema(): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            multiFormatSchemas =
                mapOf(
                    "UserCreated" to nativeProtobufUserCreatedSchema(),
                ),
            polymorphicRelationships = emptyMap(),
            channels = emptyList(),
        )

    fun generationInputWithNativeProtobufJavaMessageSchema(): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            multiFormatSchemas =
                mapOf(
                    "UserCreated" to nativeProtobufUserCreatedSchema(javaPackage = "com.example.protobuf"),
                ),
            polymorphicRelationships = emptyMap(),
            channels = emptyList(),
        )

    fun documentWithMessageHeaders(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Test API", version = "1.0.0"),
            channels =
                mapOf(
                    "userEvents" to
                        ChannelInterface.ChannelInline(
                            Channel(
                                messages =
                                    mapOf(
                                        "userSignup" to
                                            MessageInterface.MessageInline(
                                                Message(
                                                    name = "UserSignup",
                                                    payload = SchemaInterface.SchemaInline(Schema(type = "object")),
                                                    headers =
                                                        SchemaInterface.SchemaInline(
                                                            Schema(
                                                                type = "object",
                                                                properties =
                                                                    mapOf(
                                                                        "correlationId" to
                                                                            SchemaInterface.SchemaInline(Schema(type = "string")),
                                                                        "applicationInstanceId" to
                                                                            SchemaInterface.SchemaInline(Schema(type = "string")),
                                                                    ),
                                                            ),
                                                        ),
                                                ),
                                            ),
                                    ),
                            ),
                        ),
                ),
        )

    fun documentWithPolymorphicComponentAndChannel(): AsyncApiDocument {
        val externalSchema =
            Schema(
                type = "object",
                properties =
                    mapOf(
                        "status" to SchemaInterface.SchemaInline(Schema(type = "string", enum = listOf("ACTIVE"))),
                    ),
            )
        val rootSchema =
            Schema(
                oneOf =
                    listOf(
                        SchemaInterface.SchemaReference(
                            Reference(
                                ref = "#/components/schemas/External",
                                model = externalSchema,
                            ),
                        ),
                    ),
            )

        return AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Test API", version = "1.0.0"),
            channels =
                mapOf(
                    "userEvents" to
                        ChannelInterface.ChannelInline(
                            Channel(
                                address = "user.events",
                                messages =
                                    mapOf(
                                        "userCreated" to
                                            MessageInterface.MessageInline(
                                                Message(
                                                    name = "UserCreated",
                                                    payload = SchemaInterface.SchemaInline(Schema(type = "object")),
                                                ),
                                            ),
                                    ),
                            ),
                        ),
                ),
            components =
                ComponentInterface.ComponentInline(
                    Component(
                        schemas =
                            mapOf(
                                "Root" to SchemaInterface.SchemaInline(rootSchema),
                            ),
                    ),
                ),
        )
    }

    fun documentWithMultiFormatComponent(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Test API", version = "1.0.0"),
            components =
                ComponentInterface.ComponentInline(
                    Component(
                        schemas =
                            mapOf(
                                "UserCreated" to
                                    SchemaInterface.MultiFormatSchemaInline(
                                        nativeAvroUserCreatedSchema(),
                                    ),
                            ),
                    ),
                ),
        )

    fun documentWithMultiFormatMessagePayload(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Test API", version = "1.0.0"),
            channels =
                mapOf(
                    "userEvents" to
                        ChannelInterface.ChannelInline(
                            Channel(
                                address = "user.events",
                                messages =
                                    mapOf(
                                        "userCreated" to
                                            MessageInterface.MessageInline(
                                                Message(
                                                    name = "UserCreated",
                                                    payload =
                                                        SchemaInterface.MultiFormatSchemaInline(
                                                            nativeAvroUserCreatedSchema(),
                                                        ),
                                                ),
                                            ),
                                    ),
                            ),
                        ),
                ),
        )

    fun documentWithNativeProtobufComponent(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Test API", version = "1.0.0"),
            components =
                ComponentInterface.ComponentInline(
                    Component(
                        schemas =
                            mapOf(
                                "UserCreated" to
                                    SchemaInterface.MultiFormatSchemaInline(
                                        nativeProtobufUserCreatedSchema(),
                                    ),
                            ),
                    ),
                ),
        )

    private fun nativeAvroUserCreatedSchema(namespace: String? = null): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
            schema =
                linkedMapOf<String, Any>(
                    "type" to "record",
                    "name" to "UserCreated",
                ).apply {
                    if (namespace != null) {
                        put("namespace", namespace)
                    }
                    put(
                        "fields",
                        listOf(
                            linkedMapOf(
                                "name" to "userId",
                                "type" to "string",
                            ),
                            linkedMapOf(
                                "name" to "email",
                                "type" to "string",
                            ),
                        ),
                    )
                },
        )

    private fun nativeProtobufUserCreatedSchema(javaPackage: String? = null): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.google.protobuf;version=3",
            schema =
                buildString {
                    appendLine("""syntax = "proto3";""")
                    appendLine()
                    appendLine("package com.example.protobuf;")
                    appendLine()
                    if (javaPackage != null) {
                        appendLine("""option java_package = "$javaPackage";""")
                        appendLine("option java_multiple_files = true;")
                        appendLine()
                    }
                    appendLine("message UserCreated {")
                    appendLine("  string user_id = 1;")
                    appendLine("  string email = 2;")
                    appendLine("}")
                }.trimEnd(),
        )
}
