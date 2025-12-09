package dev.banking.asyncapi.generator.core.generator.avro.factory

import dev.banking.asyncapi.generator.core.generator.avro.mapper.AvroTypeMapper
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnum
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroField
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroRecord
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroSchema
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroUnion
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroUnionType
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils.toAvroDocLines
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class AvroGeneratorModelFactory(
    private val packageName: String
) {

    private val typeMapper = AvroTypeMapper(packageName)

    fun create(name: String, schema: Schema): AvroSchema? {
        if (!schema.oneOf.isNullOrEmpty()) {
            val subTypes = schema.oneOf.mapNotNull { ref ->
                if (ref is SchemaInterface.SchemaReference) {
                    val refName = ref.reference.ref.substringAfterLast('/')
                    "$packageName.${MapperUtil.toPascalCase(refName)}"
                } else null
            }

            if (subTypes.isNotEmpty()) {
                val unionTypes = subTypes.mapIndexed { index, typeName ->
                    AvroUnionType(
                        name = typeName,
                        last = index == subTypes.size - 1
                    )
                }
                return AvroUnion(
                    namespace = packageName,
                    name = name,
                    types = unionTypes
                )
            }
        }

        if (!schema.enum.isNullOrEmpty()) {
            val symbols = schema.enum.map { it.toString().trim('"', '\'') }
            val defaultSymbol = schema.default?.toString()?.trim('"', '\'')?.takeIf { symbols.contains(it) }
            val symbolWrappers = symbols.mapIndexed { index, s ->
                AvroUnionType(name = s, last = index == symbols.size - 1)
            }
            return AvroEnum(
                namespace = packageName,
                name = name,
                doc = toAvroDocLines(schema.description),
                symbols = symbolWrappers,
                default = defaultSymbol
            )
        }

        if (schema.type.getPrimaryType() != "object") {
            return null
        }

        val properties = schema.properties?.entries?.toList() ?: emptyList()

        val fields = properties.mapIndexed { index, (propName, propInterface) ->
            val (propSchema, refName) = when (propInterface) {
                is SchemaInterface.SchemaInline -> propInterface.schema to null
                is SchemaInterface.SchemaReference -> {
                    val resolved = propInterface.reference.model as? Schema
                    val name = propInterface.reference.ref.substringAfterLast('/')
                    resolved to name
                }
                else -> null to null
            }

            val isOptional = !(schema.required?.contains(propName) ?: false)

            AvroField(
                name = propName,
                doc = toAvroDocLines(propSchema?.description),
                jsonType = typeMapper.mapToAvroType(propSchema, isOptional, refName),
                last = index == properties.size - 1,
                hasDefaultNull = isOptional
            )
        }

        return AvroRecord(
            namespace = packageName,
            name = name,
            doc = toAvroDocLines(schema.description),
            fields = fields
        )
    }
}
