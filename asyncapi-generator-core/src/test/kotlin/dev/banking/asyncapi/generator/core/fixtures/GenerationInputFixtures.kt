package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.fixtures.analyzedMessage
import dev.banking.asyncapi.generator.core.fixtures.nativeMessage
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
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
        generationInputOf(
            schemas = emptyMap(),
            polymorphicRelationships = emptyMap(),
            channels =
                listOf(
                    AnalyzedChannel(
                        channelName = "userEvents",
                        topic = "user.events",
                        messages =
                            listOf(
                                analyzedMessage(
                                    messageName = "UserSignedUp",
                                    payloadTypeName = "UserSignedUpPayload",
                                    schema = Schema(type = JsonSchemaType.Single("object")),
                                ),
                            ),
                    ),
                ),
        )

    fun generationInputWithObjectEnumAndPrimitive(): GenerationInput {
        val userSchema =
            Schema(
                type = JsonSchemaType.Single("object"),
                required = listOf("id"),
                properties =
                    mapOf(
                        "id" to SchemaInterface.SchemaInline(Schema(type = JsonSchemaType.Single("string"))),
                    ),
            )
        val statusSchema =
            Schema(
                type = JsonSchemaType.Single("string"),
                enum = listOf("ACTIVE", "INACTIVE"),
            )

        return generationInputOf(
            schemas =
                linkedMapOf(
                    "User" to userSchema,
                    "Status" to statusSchema,
                    "IgnoredPrimitive" to Schema(type = JsonSchemaType.Single("string")),
                ),
            polymorphicRelationships = mapOf("User" to listOf("Command")),
            channels = emptyList(),
        )
    }

    fun generationInputWithNativeAvroSchema(): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            schemaDeclarations =
                SchemaDeclarationCatalog(
                    multiFormatSchemas =
                        mapOf(
                            "UserCreated" to nativeAvroUserCreatedSchema(namespace = "com.example.avro"),
                        ),
                ),
            polymorphicRelationships = emptyMap(),
            channels = emptyList(),
        )

    fun generationInputWithNativeAvroMessageAndObjectKey(): GenerationInput {
        val keySchema = accountKeySchema()
        val nativeSchema = nativeAvroUserCreatedSchema(namespace = "com.example.avro")
        return GenerationInput(
            schemas = mapOf("UserCreatedKey" to keySchema),
            schemaDeclarations =
                SchemaDeclarationCatalog(
                    multiFormatSchemas = mapOf("UserCreated" to nativeSchema),
                ),
            polymorphicRelationships = emptyMap(),
            channels =
                listOf(
                    AnalyzedChannel(
                        channelName = "userEvents",
                        topic = "user.events",
                        messages =
                            listOf(
                                nativeMessage(
                                    messageName = "UserCreated",
                                    payloadName = "UserCreated",
                                    schema = nativeSchema,
                                    keySchema = SchemaInterface.SchemaInline(keySchema),
                                ),
                            ),
                    ),
                ),
        )
    }

    fun generationInputWithNativeProtobufSchema(): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            schemaDeclarations =
                SchemaDeclarationCatalog(
                    multiFormatSchemas =
                        mapOf(
                            "UserCreated" to nativeProtobufUserCreatedSchema(),
                        ),
                ),
            polymorphicRelationships = emptyMap(),
            channels = emptyList(),
        )

    fun generationInputWithNativeProtobufJavaMessageSchema(): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            schemaDeclarations =
                SchemaDeclarationCatalog(
                    multiFormatSchemas =
                        mapOf(
                            "UserCreated" to nativeProtobufUserCreatedSchema(javaPackage = "com.example.protobuf"),
                        ),
                ),
            polymorphicRelationships = emptyMap(),
            channels = emptyList(),
        )

    fun generationInputWithJsonSchemas(): GenerationInput {
        val addressSchema =
            Schema(
                type = JsonSchemaType.Single("object"),
                required = listOf("city"),
                properties =
                    mapOf(
                        "city" to SchemaInterface.SchemaInline(Schema(type = JsonSchemaType.Single("string"))),
                    ),
            )
        val accountSchema =
            Schema(
                id = "urn:example:my-account",
                comment = "Generated from the AsyncAPI account payload.",
                type = JsonSchemaType.Single("object"),
                required = listOf("accountId"),
                properties =
                    linkedMapOf(
                        "accountId" to
                            SchemaInterface.SchemaInline(
                                Schema(
                                    type = JsonSchemaType.Multiple(listOf("string", "null")),
                                    default = null,
                                    defaultSet = true,
                                ),
                            ),
                        "address" to
                            SchemaInterface.SchemaReference(
                                Reference(
                                    ref = "#/components/schemas/Address",
                                    model = addressSchema,
                                ),
                            ),
                    ),
                ifSchema =
                    SchemaInterface.SchemaInline(
                        Schema(
                            properties =
                                mapOf(
                                    "accountId" to
                                        SchemaInterface.SchemaInline(
                                            Schema(type = JsonSchemaType.Single("string"), pattern = "^A"),
                                        ),
                                ),
                        ),
                    ),
                thenSchema = SchemaInterface.SchemaInline(Schema(required = listOf("address"))),
                discriminator = "accountType",
                deprecated = true,
            )

        return generationInputOf(
            schemas =
                linkedMapOf(
                    "MyAccount" to accountSchema,
                    "Address" to addressSchema,
                ),
            polymorphicRelationships = emptyMap(),
            channels = emptyList(),
        )
    }

    fun generationInputWithNativeJsonSchema(): GenerationInput =
        GenerationInput(
            schemas = emptyMap(),
            schemaDeclarations =
                SchemaDeclarationCatalog(
                    multiFormatSchemas =
                        mapOf(
                            "MyAccount" to
                                MultiFormatSchema(
                                    schemaFormat = "application/schema+json;version=draft-07",
                                    schema =
                                        linkedMapOf(
                                            "type" to "object",
                                            "properties" to
                                                mapOf(
                                                    "address" to
                                                        mapOf(
                                                            $$"$ref" to "#/components/schemas/Address",
                                                        ),
                                                ),
                                        ),
                                ),
                        ),
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
                                                    payload =
                                                        SchemaInterface.SchemaInline(
                                                            Schema(type = JsonSchemaType.Single("object")),
                                                        ),
                                                    headers =
                                                        SchemaInterface.SchemaInline(
                                                            Schema(
                                                                type = JsonSchemaType.Single("object"),
                                                                properties =
                                                                    mapOf(
                                                                        "correlationId" to
                                                                            SchemaInterface.SchemaInline(
                                                                                Schema(
                                                                                    type =
                                                                                        JsonSchemaType.Single(
                                                                                            "string",
                                                                                        ),
                                                                                ),
                                                                            ),
                                                                        "applicationInstanceId" to
                                                                            SchemaInterface.SchemaInline(
                                                                                Schema(
                                                                                    type =
                                                                                        JsonSchemaType.Single(
                                                                                            "string",
                                                                                        ),
                                                                                ),
                                                                            ),
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
                type = JsonSchemaType.Single("object"),
                properties =
                    mapOf(
                        "status" to
                            SchemaInterface.SchemaInline(
                                Schema(type = JsonSchemaType.Single("string"), enum = listOf("ACTIVE")),
                            ),
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
                                                    payload =
                                                        SchemaInterface.SchemaInline(
                                                            Schema(type = JsonSchemaType.Single("object")),
                                                        ),
                                                ),
                                            ),
                                    ),
                            ),
                        ),
                ),
            components =
                Component(
                    schemas =
                        mapOf(
                            "Root" to SchemaInterface.SchemaInline(rootSchema),
                        ),
                ),
        )
    }

    fun documentWithMultiFormatComponent(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Test API", version = "1.0.0"),
            components =
                Component(
                    schemas =
                        mapOf(
                            "UserCreated" to
                                SchemaInterface.MultiFormatSchemaInline(
                                    nativeAvroUserCreatedSchema(),
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

    fun documentWithNativeAvroMessageAndObjectKey(): AsyncApiDocument =
        documentWithNativeMessageAndObjectKey(
            payload = nativeAvroUserCreatedSchema(namespace = "com.example.avro"),
        )

    fun documentWithNativeProtobufMessageAndObjectKey(): AsyncApiDocument =
        documentWithNativeMessageAndObjectKey(
            payload =
                nativeProtobufUserCreatedSchema(
                    javaPackage = "com.example.protobuf",
                    messageName = "UserCreatedPayload",
                ),
        )

    private fun documentWithNativeMessageAndObjectKey(payload: MultiFormatSchema): AsyncApiDocument {
        val keySchema = accountKeySchema()
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
                                                    payload = SchemaInterface.MultiFormatSchemaInline(payload),
                                                    bindings =
                                                        mapOf(
                                                            "kafka" to
                                                                BindingInterface.BindingInline(
                                                                    kafkaKeyBinding(
                                                                        SchemaInterface.SchemaInline(keySchema),
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
    }

    fun documentWithNativeProtobufComponent(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Test API", version = "1.0.0"),
            components =
                Component(
                    schemas =
                        mapOf(
                            "UserCreated" to
                                SchemaInterface.MultiFormatSchemaInline(
                                    nativeProtobufUserCreatedSchema(),
                                ),
                        ),
                ),
        )

    fun documentWithNativeProtobufJavaMessageComponent(): AsyncApiDocument =
        AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Test API", version = "1.0.0"),
            components =
                Component(
                    schemas =
                        mapOf(
                            "UserCreated" to
                                SchemaInterface.MultiFormatSchemaInline(
                                    nativeProtobufUserCreatedSchema(javaPackage = "com.example.protobuf"),
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

    private fun accountKeySchema(): Schema =
        Schema(
            type = JsonSchemaType.Single("object"),
            required = listOf("tenantId", "accountId"),
            properties =
                linkedMapOf(
                    "tenantId" to SchemaInterface.SchemaInline(Schema(type = JsonSchemaType.Single("string"))),
                    "accountId" to SchemaInterface.SchemaInline(Schema(type = JsonSchemaType.Single("string"))),
                ),
        )

    private fun nativeProtobufUserCreatedSchema(
        javaPackage: String? = null,
        messageName: String = "UserCreated",
    ): MultiFormatSchema =
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
                    appendLine("message $messageName {")
                    appendLine("  string user_id = 1;")
                    appendLine("  string email = 2;")
                    appendLine("}")
                }.trimEnd(),
        )
}

/** A [GenerationInput] whose contract declarations are its source schemas. */
internal fun generationInputOf(
    schemas: Map<String, Schema>,
    polymorphicRelationships: Map<String, List<String>> = emptyMap(),
    channels: List<AnalyzedChannel> = emptyList(),
): GenerationInput =
    GenerationInput(
        schemas = schemas,
        schemaDeclarations = SchemaDeclarationCatalog(asyncApiSchemas = schemas),
        polymorphicRelationships = polymorphicRelationships,
        channels = channels,
    )
