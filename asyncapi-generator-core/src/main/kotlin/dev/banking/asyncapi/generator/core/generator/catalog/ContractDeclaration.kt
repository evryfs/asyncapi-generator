package dev.banking.asyncapi.generator.core.generator.catalog

import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * A schema declared under a generated name. Two declarations of one name are
 * the same declaration when they have the same identity, and collide otherwise.
 */
internal sealed interface ContractDeclaration {
    fun hasSameIdentity(other: ContractDeclaration): Boolean

    class AsyncApi(
        val schema: Schema,
        val identity: ContractDeclarationIdentity?,
    ) : ContractDeclaration {
        override fun hasSameIdentity(other: ContractDeclaration): Boolean =
            other is AsyncApi &&
                ((identity != null && identity == other.identity) || schema === other.schema)
    }

    class MultiFormat(
        val schema: MultiFormatSchema,
        val identity: ContractDeclarationIdentity?,
    ) : ContractDeclaration {
        override fun hasSameIdentity(other: ContractDeclaration): Boolean =
            other is MultiFormat &&
                ((identity != null && identity == other.identity) || schema === other.schema)
    }

    class BooleanSchema(
        val value: Boolean,
    ) : ContractDeclaration {
        override fun hasSameIdentity(other: ContractDeclaration): Boolean =
            other is BooleanSchema && value == other.value
    }
}

/**
 * Where a declaration comes from, so that the same schema reached twice is not a collision.
 */
internal sealed interface ContractDeclarationIdentity {
    data class ComponentSchema(
        val name: String,
    ) : ContractDeclarationIdentity

    data class ComponentMessagePayload(
        val name: String,
    ) : ContractDeclarationIdentity

    data class ReferencedMessagePayload(
        val sourceId: String?,
        val ref: String,
    ) : ContractDeclarationIdentity

    data class ComponentMessageKafkaKey(
        val name: String,
    ) : ContractDeclarationIdentity

    data class ReferencedMessageKafkaKey(
        val sourceId: String?,
        val ref: String,
    ) : ContractDeclarationIdentity

    data class ReferenceTarget(
        val sourceId: String?,
        val ref: String,
    ) : ContractDeclarationIdentity
}
