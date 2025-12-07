package com.tietoevry.banking.asyncapi.generator.core.bundler

import com.tietoevry.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import com.tietoevry.banking.asyncapi.generator.core.model.channels.Channel
import com.tietoevry.banking.asyncapi.generator.core.model.channels.ChannelInterface
import com.tietoevry.banking.asyncapi.generator.core.model.components.Component
import com.tietoevry.banking.asyncapi.generator.core.model.components.ComponentInterface
import com.tietoevry.banking.asyncapi.generator.core.model.info.Info
import com.tietoevry.banking.asyncapi.generator.core.model.messages.Message
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageInterface
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import com.tietoevry.banking.asyncapi.generator.core.model.servers.Server
import com.tietoevry.banking.asyncapi.generator.core.model.servers.ServerInterface
import com.tietoevry.banking.asyncapi.generator.core.model.tags.Tag
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface

fun expectedMultiFileBundled(): AsyncApiDocument =
    AsyncApiDocument(
        asyncapi = "3.0.0",
        info = Info(
            title = "Simple Schema Validation Test",
            version = "1.0.0"
        ),
        servers = mapOf(
            "scram-connections" to ServerInterface.ServerInline(expectedScramServer()),
            "mtls-connections" to ServerInterface.ServerInline(expectedMtlsServer())
        ),
        channels = mapOf(
            "testChannel" to ChannelInterface.ChannelInline(expectedTestChannel()),
            "auditChannel" to ChannelInterface.ChannelInline(expectedAuditChannel()),
            "externalAuditChannel" to ChannelInterface.ChannelReference(expectedExternalAuditChannelRef())
        ),
        components = ComponentInterface.ComponentInline(
            Component(
                messages = mapOf(
                    "testMessage" to MessageInterface.MessageInline(expectedTestMessage()),
                    "auditMessage" to MessageInterface.MessageInline(expectedAuditMessage())
                )
            )
        )
    )

private fun expectedScramServer(): Server =
    Server(
        host = "\"kafka.scram.local:9092",
        protocol = "kafka",
        description = "SCRAM-based Kafka cluster",
        tags = listOf(
            expectedSharedTagRef(),
            expectedScramTagRef()
        )
    )

private fun expectedMtlsServer(): Server =
    Server(
        host = "\"kafka.mtls.local:9093",
        protocol = "kafka-secure",
        description = "mTLS-protected Kafka cluster",
        tags = listOf(
            expectedSharedTagRef(),
            expectedMtlsTagRef()
        )
    )

private fun expectedSharedTagRef(): TagInterface =
    TagInterface.TagReference(
        Reference("'asyncapi_multifile_example_tags.yaml#/components/tags/shared").apply {
            model = Tag(
                name = "shared",
                description = "Shared server tag used across environments"
            )
        }
    )

private fun expectedScramTagRef(): TagInterface =
    TagInterface.TagReference(
        Reference("'asyncapi_multifile_example_tags.yaml#/components/tags/scram").apply {
            model = Tag(
                name = "scram",
                description = "SCRAM-based Kafka cluster"
            )
        }
    )

private fun expectedMtlsTagRef(): TagInterface =
    TagInterface.TagReference(
        Reference("'asyncapi_multifile_example_tags.yaml#/components/tags/mtls").apply {
            model = Tag(
                name = "mtls",
                description = "mTLS-protected Kafka cluster"
            )
        }
    )

private fun expectedValidKotlinUserSchema(): Schema =
    Schema(
        title = "Valid User Schema",
        description = "Represents a user with valid fields",
        type = "object",
        required = listOf("id", "username"),
        properties = mapOf(
            "id" to SchemaInterface.SchemaInline(
                Schema(
                    description = "The unique identifier of the user",
                    type = "integer"
                )
            ),
            "username" to SchemaInterface.SchemaInline(
                Schema(
                    description = "Username between 3 and 20 characters",
                    type = "string",
                    maxLength = 20,
                    minLength = 3
                )
            ),
            "active" to SchemaInterface.SchemaInline(
                Schema(
                    description = "Whether the user is active",
                    type = "boolean"
                )
            ),
            "email" to SchemaInterface.SchemaInline(
                Schema(
                    description = "Optional email address",
                    type = "string"
                )
            )
        )
    )

private fun expectedAuditMetadataSchema(): Schema =
    Schema(
        title = "Audit Metadata",
        description = "Metadata about changes to user records",
        type = "object",
        required = listOf("timestamp", "actor"),
        properties = mapOf(
            "timestamp" to SchemaInterface.SchemaInline(
                Schema(
                    description = "When the change was made",
                    type = "string",
                    format = "date-time"
                )
            ),
            "actor" to SchemaInterface.SchemaInline(
                Schema(
                    description = "Who made the change",
                    type = "string"
                )
            ),
            "reason" to SchemaInterface.SchemaInline(
                Schema(
                    description = "Optional reason for the change",
                    type = "string"
                )
            )
        )
    )

private fun expectedTestMessage(): Message =
    Message(
        name = "\"testMessage",
        payload = SchemaInterface.SchemaReference(
            Reference("'asyncapi_multifile_example_schemas.yaml#/components/schemas/validKotlinUserSchema").apply {
                model = expectedValidKotlinUserSchema()
            }
        )
    )

private fun expectedAuditMessage(): Message =
    Message(
        name = "\"auditMessage",
        payload = SchemaInterface.SchemaReference(
            Reference("'asyncapi_multifile_example_schemas.yaml#/components/schemas/auditMetadata").apply {
                model = expectedAuditMetadataSchema()
            }
        )
    )

private fun expectedTestChannel(): Channel =
    Channel(
        address = "\"example.topic",
        messages = mapOf(
            "testMessage" to MessageInterface.MessageReference(
                Reference("\"#/components/messages/testMessage").apply {
                    model = expectedTestMessage()
                }
            )
        )
    )

private fun expectedAuditChannel(): Channel =
    Channel(
        address = "\"example.audit",
        messages = mapOf(
            "auditMessage" to MessageInterface.MessageReference(
                Reference("\"#/components/messages/auditMessage").apply {
                    model = expectedAuditMessage()
                }
            )
        )
    )

private fun expectedExternalAuditChannelRef(): Reference =
    Reference("'asyncapi_multifile_example_channels.yaml#/channels/externalAuditChannel").apply {
        model = Channel(
            address = "\"external.audit",
            messages = mapOf(
                "externalAuditMessage" to MessageInterface.MessageInline(
                    Message(
                        name = "\"ExternalAuditMessage",
                        payload = SchemaInterface.SchemaReference(
                            Reference("'asyncapi_multifile_example_schemas.yaml#/components/schemas/auditMetadata").apply {
                                model = expectedAuditMetadataSchema()
                            }
                        )
                    )
                )
            ),
            description = "\"External audit events channel defined in shared file"
        )
    }
