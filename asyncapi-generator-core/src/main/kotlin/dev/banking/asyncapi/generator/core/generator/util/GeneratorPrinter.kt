package dev.banking.asyncapi.generator.core.generator.util

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.collections.get

object GeneratorPrinter {

    fun debugSchemasPrinter(schemas: Map<String, Schema>) {
        schemas.forEach { (name, schema) ->
            println("Schema: $name")
            schema.properties?.forEach { (propName, propSchemaInterface) ->
                var propDefault: Any? = "N/A"
                var propType: Any? = "N/A"
                var propEnum: Any? = "N/A"
                when (propSchemaInterface) {
                    is SchemaInterface.SchemaInline -> {
                        val inlineSchema = propSchemaInterface.schema
                        propDefault = inlineSchema.default
                        propType = inlineSchema.type
                        propEnum = inlineSchema.enum
                    }
                    is SchemaInterface.SchemaReference -> {
                        val model = propSchemaInterface.reference.model
                        // Check if the model is a Map (which is how Jackson parses generic objects)
                        if (model is Map<*, *>) {
                            propDefault = model["default"]
                            propType = model["type"]
                            propEnum = model["enum"]
                        } else if (model is Schema) {
                            // Or maybe it's already a Schema object
                            propDefault = model.default
                            propType = model.type
                            propEnum = model.enum
                        }
                    }
                    else -> { /* do nothing */
                    }
                }
                println("  - Property: '$propName', Type: $propType, Default: $propDefault, Enum: $propEnum")
            }
        }
    }

    fun debugSchemaInterfacesPrinter(schemaInterfacesMap: Map<String, SchemaInterface>) {
        schemaInterfacesMap.forEach { (name, propSchemaInterface) ->
            println("--- SchemaInterface Map Entry: $name ---")

            var propDefault: Any? = "N/A"
            var propType: Any? = "N/A"
            var propEnum: Any? = "N/A"
            var propRef: String? = "N/A"
            var propIsInline: Boolean = false
            var hasModel: Boolean = false // To indicate if a reference has a model

            when (propSchemaInterface) {
                is SchemaInterface.SchemaInline -> {
                    propIsInline = true
                    val inlineSchema = propSchemaInterface.schema
                    propDefault = inlineSchema.default
                    propType = inlineSchema.type?.getPrimaryType()
                    propEnum = inlineSchema.enum
                }
                is SchemaInterface.SchemaReference -> {
                    propRef = propSchemaInterface.reference.ref
                    val model = propSchemaInterface.reference.model
                    hasModel = model != null

                    if (model is Schema) {
                        // It's already a Schema object
                        propDefault = model.default
                        propType = model.type?.getPrimaryType()
                        propEnum = model.enum
                    } else if (model is Map<*, *>) {
                        // It's a generic map (if Jackson hasn't fully deserialized to Schema yet)
                        propDefault = model["default"]
                        propType = model["type"]
                        propEnum = model["enum"]
                    }
                }
                is SchemaInterface.BooleanSchema -> {
                    propType = "boolean"
                    propDefault = propSchemaInterface.value
                }
                is SchemaInterface.MultiFormatSchemaInline -> {
                    propType = "multi-format-schema"
                }
            }
            println("  - IsInline: $propIsInline, Type: $propType, Default: $propDefault, Enum: $propEnum, Ref: $propRef, HasModel: $hasModel")
        }
    }
}
