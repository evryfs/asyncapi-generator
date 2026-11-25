package dev.banking.asyncapi.generator.core.generator.catalog

import dev.banking.asyncapi.generator.core.generator.ir.NameCase
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Declares every schema in `components.schemas` under its generated name.
 *
 * A schema that messages use only in headers gets no source model.
 */
internal object ComponentSchemaCollector {
    /** Declares the component schemas of [components]. */
    fun collect(
        components: Component?,
        usage: SchemaUsageIndex,
        declarations: SchemaDeclarations,
    ) {
        components?.schemas?.forEach { (name, schemaInterface) -> declare(name, schemaInterface, usage, declarations) }
    }

    private fun declare(
        name: String,
        schemaInterface: SchemaInterface,
        usage: SchemaUsageIndex,
        declarations: SchemaDeclarations,
    ) {
        val schemaName = NameCase.toPascalCase(name)
        val identity = ContractDeclarationIdentity.ComponentSchema(name)
        val origin = "component schema components.schemas['$name']"
        val generatesSourceModel = !usage.isHeaderOnly(schemaName)
        val declared =
            when (schemaInterface) {
                is SchemaInterface.SchemaInline -> schemaInterface.schema
                is SchemaInterface.MultiFormatSchemaInline -> schemaInterface.multiFormatSchema
                is SchemaInterface.BooleanSchema -> schemaInterface
                is SchemaInterface.SchemaReference -> schemaInterface.reference.model
            }
        when (declared) {
            is Schema -> declarations.declareSchema(schemaName, declared, identity, origin, generatesSourceModel)
            is MultiFormatSchema -> declarations.declareMultiFormat(schemaName, declared, identity, origin)
            is SchemaInterface.BooleanSchema -> declarations.declareBoolean(schemaName, declared.value, origin)
            else -> Unit
        }
    }
}
