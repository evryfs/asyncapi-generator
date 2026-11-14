package dev.banking.asyncapi.generator.core.generator.avro.factory

import com.fasterxml.jackson.core.io.JsonStringEncoder
import dev.banking.asyncapi.generator.core.generator.avro.mapper.AvroTypeMapper
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnum
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnumSymbol
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroField
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroRecord
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroSchema
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils.toAvroDocLines
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidAvroName
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Builds Avro records and enums from analyzed JSON schemas.
 *
 * Names that Avro rejects fail generation, because renaming them would silently change a schema
 * that may already be registered. Text values are JSON-escaped for the `.avsc` templates.
 */
class AvroGeneratorModelFactory(
    private val packageName: String,
) {
    private val typeMapper = AvroTypeMapper(packageName)

    fun create(name: String, schema: Schema): AvroSchema? {
        if (schema.oneOf.orEmpty().any { schemaOption -> schemaOption is SchemaInterface.SchemaReference }) {
            return null
        }

        if (!schema.enum.isNullOrEmpty()) {
            requireAvroName("enum", name, "components.schemas.$name")
            val symbols = schema.enum.map { it.toString().trim('"', '\'') }
            symbols.forEach { requireAvroName("enum symbol", it, "components.schemas.$name") }
            val defaultSymbol = schema.default?.toString()?.trim('"', '\'')?.takeIf { symbols.contains(it) }
            val symbolWrappers = symbols.mapIndexed { index, s ->
                AvroEnumSymbol(name = s, last = index == symbols.size - 1)
            }
            return AvroEnum(
                namespace = packageName,
                name = name,
                doc = jsonText(toAvroDocLines(schema.description)),
                symbols = symbolWrappers,
                default = defaultSymbol,
            )
        }

        if (schema.type.getPrimaryType() != "object") {
            return null
        }
        requireAvroName("record", name, "components.schemas.$name")

        val properties = schema.properties?.entries?.toList() ?: emptyList()

        val fields = properties.mapIndexed { index, (propName, propInterface) ->
            requireAvroName("field", propName, "components.schemas.$name")
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
            val defaultJson = propSchema?.let { defaultJson(it, refName) }

            AvroField(
                name = propName,
                doc = jsonText(toAvroDocLines(propSchema?.description)),
                jsonType =
                    when {
                        // A union's default must match its first branch, so a non-null default comes first.
                        isOptional && defaultJson != null ->
                            typeMapper.mapToAvroType(propSchema, isOptional = true, refName)
                                .replaceFirst("[\"null\", ", "[")
                                .removeSuffix("]") + ", \"null\"]"
                        else -> typeMapper.mapToAvroType(propSchema, isOptional, refName)
                    },
                last = index == properties.size - 1,
                defaultJson = defaultJson ?: "null".takeIf { isOptional },
            )
        }

        return AvroRecord(
            namespace = packageName,
            name = name,
            doc = jsonText(toAvroDocLines(schema.description)),
            fields = fields,
        )
    }

    /**
     * The field's default as Avro default JSON, for scalars and enum symbols. Other defaults are
     * left out: Avro encodes them differently from JSON Schema (logical types, records).
     */
    private fun defaultJson(schema: Schema, refName: String?): String? {
        val value = schema.default ?: return null
        val text = value as? String ?: value.toString()
        return when {
            !schema.enum.isNullOrEmpty() -> "\"${jsonText(text)}\"".takeIf { schema.enum.map(Any?::toString).contains(text) }
            refName != null -> null
            schema.format != null -> null
            else ->
                when (schema.type.getPrimaryType()) {
                    "string" -> "\"${jsonText(text)}\""
                    "boolean" -> text.takeIf { it == "true" || it == "false" }
                    "integer", "number" -> text.takeIf { it.toBigDecimalOrNull() != null }
                    else -> null
                }
        }
    }

    private fun requireAvroName(kind: String, value: String, location: String) {
        if (!AVRO_NAME.matches(value)) throw InvalidAvroName(kind = kind, avroName = value, location = location)
    }

    private fun jsonText(text: String?): String? = text?.let { String(JsonStringEncoder.getInstance().quoteAsString(it)) }

    private companion object {
        val AVRO_NAME = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }
}
