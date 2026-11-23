package dev.banking.asyncapi.generator.core.generator.catalog

import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SchemaNameCollision
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * Collects declared schemas under their generated names.
 *
 * Source models and contract declarations claim names separately: a Kafka key
 * model is only a source model, and a header-only schema is only a contract
 * declaration. Within each, a second declaration of a name with another
 * identity fails with [SchemaNameCollision].
 */
internal class SchemaDeclarations {
    private val sourceModels = mutableMapOf<String, Schema>()
    private val sourceModelOwners = mutableMapOf<String, Owner>()
    private val asyncApiSchemas = mutableMapOf<String, Schema>()
    private val multiFormatSchemas = mutableMapOf<String, MultiFormatSchema>()
    private val booleanSchemas = mutableMapOf<String, Boolean>()
    private val contractOwners = mutableMapOf<String, Owner>()

    /** Declares an AsyncAPI schema, and a source model for it when [generatesSourceModel] is set. */
    fun declareSchema(
        generatedName: String,
        schema: Schema,
        identity: ContractDeclarationIdentity?,
        origin: String,
        generatesSourceModel: Boolean = true,
    ) {
        declareContract(generatedName, ContractDeclaration.AsyncApi(schema, identity), origin)
        if (generatesSourceModel) declareSourceModel(generatedName, schema, identity, origin)
    }

    /** Declares a Multi Format Schema, which has no source model. */
    fun declareMultiFormat(
        generatedName: String,
        schema: MultiFormatSchema,
        identity: ContractDeclarationIdentity?,
        origin: String,
    ) = declareContract(generatedName, ContractDeclaration.MultiFormat(schema, identity), origin)

    /** Declares a boolean schema, which has no source model. */
    fun declareBoolean(
        generatedName: String,
        value: Boolean,
        origin: String,
    ) = declareContract(generatedName, ContractDeclaration.BooleanSchema(value), origin)

    /** Declares a source model that is not a contract declaration, such as a Kafka key model. */
    fun declareSourceModel(
        generatedName: String,
        schema: Schema,
        identity: ContractDeclarationIdentity?,
        origin: String,
    ) {
        if (claim(generatedName, ContractDeclaration.AsyncApi(schema, identity), origin, sourceModelOwners)) {
            sourceModels[generatedName] = schema
        }
    }

    /** The source models and contract declarations collected so far. */
    fun toLoadedSchemas(): LoadedSchemas =
        LoadedSchemas(
            schemas = sourceModels.toMap(),
            schemaDeclarations =
                SchemaDeclarationCatalog(
                    asyncApiSchemas = asyncApiSchemas.toMap(),
                    multiFormatSchemas = multiFormatSchemas.toMap(),
                    booleanSchemas = booleanSchemas.toMap(),
                ),
        )

    private fun declareContract(
        generatedName: String,
        declaration: ContractDeclaration,
        origin: String,
    ) {
        if (!claim(generatedName, declaration, origin, contractOwners)) return
        when (declaration) {
            is ContractDeclaration.AsyncApi -> asyncApiSchemas[generatedName] = declaration.schema
            is ContractDeclaration.MultiFormat -> multiFormatSchemas[generatedName] = declaration.schema
            is ContractDeclaration.BooleanSchema -> booleanSchemas[generatedName] = declaration.value
        }
    }

    /** Claims [generatedName] for [declaration]; `false` when the same declaration already holds it. */
    private fun claim(
        generatedName: String,
        declaration: ContractDeclaration,
        origin: String,
        owners: MutableMap<String, Owner>,
    ): Boolean {
        val existingOwner = owners[generatedName]
        if (existingOwner != null) {
            if (!existingOwner.declaration.hasSameIdentity(declaration)) {
                throw SchemaNameCollision(
                    generatedName = generatedName,
                    firstOrigin = existingOwner.origin,
                    conflictingOrigin = origin,
                )
            }
            return false
        }
        owners[generatedName] = Owner(declaration, origin)
        return true
    }

    /**
     * The first declaration of a name.
     *
     * @param declaration the declaration that holds the name.
     * @param origin where it was declared, for collision messages.
     */
    private data class Owner(
        val declaration: ContractDeclaration,
        val origin: String,
    )
}
