package dev.banking.asyncapi.generator.core.generator.kafka

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Selects generated object-key models and their schema dependencies.
 */
internal object KafkaKeyModelSelector {
    fun select(input: GenerationInput): Map<String, Schema> {
        val selected = linkedMapOf<String, Schema>()

        input.channels.forEach { channel ->
            channel.messages.forEach { message ->
                collectKeyModel(
                    messageName = message.messageName,
                    keySchema = message.keySchema,
                    availableSchemas = input.schemas,
                    selectedSchemas = selected,
                )
            }
            channel.multiFormatMessages.forEach { message ->
                collectKeyModel(
                    messageName = message.messageName,
                    keySchema = message.keySchema,
                    availableSchemas = input.schemas,
                    selectedSchemas = selected,
                )
            }
        }

        return selected
    }

    private fun collectKeyModel(
        messageName: String,
        keySchema: SchemaInterface?,
        availableSchemas: Map<String, Schema>,
        selectedSchemas: MutableMap<String, Schema>,
    ) {
        if (keySchema == null) return
        val keyModel = KafkaKeySchemaResolver.resolveObjectModelOrNull(messageName, keySchema) ?: return
        collectNamedSchema(
            schemaName = keyModel.name,
            availableSchemas = availableSchemas,
            selectedSchemas = selectedSchemas,
        )
    }

    private fun collectNamedSchema(
        schemaName: String,
        availableSchemas: Map<String, Schema>,
        selectedSchemas: MutableMap<String, Schema>,
    ) {
        if (schemaName in selectedSchemas) return
        val schema = availableSchemas[schemaName] ?: return
        selectedSchemas[schemaName] = schema
        collectSchemaDependencies(
            schema = schema,
            availableSchemas = availableSchemas,
            selectedSchemas = selectedSchemas,
        )
    }

    private fun collectSchemaDependencies(
        schema: Schema,
        availableSchemas: Map<String, Schema>,
        selectedSchemas: MutableMap<String, Schema>,
    ) {
        schema.properties?.values?.forEach { property ->
            collectSchemaDependency(property, availableSchemas, selectedSchemas)
        }
        schema.items?.let { items ->
            collectSchemaDependency(items, availableSchemas, selectedSchemas)
        }
        schema.additionalProperties?.let { additionalProperties ->
            collectSchemaDependency(additionalProperties, availableSchemas, selectedSchemas)
        }
        schema.oneOf?.forEach { candidate ->
            collectSchemaDependency(candidate, availableSchemas, selectedSchemas)
        }
        schema.anyOf?.forEach { candidate ->
            collectSchemaDependency(candidate, availableSchemas, selectedSchemas)
        }
        schema.allOf?.forEach { candidate ->
            collectSchemaDependency(candidate, availableSchemas, selectedSchemas)
        }
        schema.not?.let { excluded ->
            collectSchemaDependency(excluded, availableSchemas, selectedSchemas)
        }
    }

    private fun collectSchemaDependency(
        schemaInterface: SchemaInterface,
        availableSchemas: Map<String, Schema>,
        selectedSchemas: MutableMap<String, Schema>,
    ) {
        when (schemaInterface) {
            is SchemaInterface.SchemaInline ->
                collectSchemaDependencies(
                    schema = schemaInterface.schema,
                    availableSchemas = availableSchemas,
                    selectedSchemas = selectedSchemas,
                )
            is SchemaInterface.SchemaReference ->
                collectNamedSchema(
                    schemaName = MapperUtil.toPascalCase(schemaInterface.reference.ref.substringAfterLast('/')),
                    availableSchemas = availableSchemas,
                    selectedSchemas = selectedSchemas,
                )
            is SchemaInterface.BooleanSchema,
            is SchemaInterface.MultiFormatSchemaInline,
            -> Unit
        }
    }
}
