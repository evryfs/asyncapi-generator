package dev.banking.asyncapi.generator.core.generator.avro.factory

import com.fasterxml.jackson.core.io.JsonStringEncoder
import dev.banking.asyncapi.generator.core.generator.avro.mapper.AvroTypeMapper
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnum
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnumSymbol
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroField
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroRecord
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroSchema
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroType
import dev.banking.asyncapi.generator.core.generator.sourcemodel.DefaultValue
import dev.banking.asyncapi.generator.core.generator.sourcemodel.EnumModel
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelDefinition
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ObjectModel
import dev.banking.asyncapi.generator.core.generator.sourcemodel.OpenObjectModel
import dev.banking.asyncapi.generator.core.generator.sourcemodel.PropertyDefinition
import dev.banking.asyncapi.generator.core.generator.sourcemodel.UnionModel
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidAvroName

/**
 * Builds Avro records and enums from the models of a contract.
 *
 * Objects become records and enums become enums; unions have no schema of their own, as fields hold
 * their members directly. Names that Avro rejects fail generation, because renaming them would
 * silently change a schema that may already be registered. Text values are JSON-escaped for the
 * `.avsc` templates.
 *
 * @param packageName the Avro namespace of the generated schemas
 * @param models the models of the contract, named with [dev.banking.asyncapi.generator.core.generator.avro.AvroNaming]
 */
internal class AvroGeneratorModelFactory(
    private val packageName: String,
    models: List<ModelDefinition>,
) {
    private val typeMapper = AvroTypeMapper(packageName, models)
    private val enumValues =
        models.filterIsInstance<EnumModel>().associate { model -> model.name to model.values() }

    /** The Avro schema of [model]; null for a union. */
    fun create(model: ModelDefinition): AvroSchema? =
        when (model) {
            is EnumModel -> enum(model)
            is ObjectModel -> record(model.name, model.description, model.properties)
            is OpenObjectModel -> record(model.name, model.description, emptyList())
            is UnionModel -> null
        }

    private fun enum(model: EnumModel): AvroEnum {
        requireAvroName("enum", model.name, model.name)
        val symbols = model.values()
        symbols.forEach { requireAvroName("enum symbol", it, model.name) }
        return AvroEnum(
            namespace = packageName,
            name = model.name,
            doc = doc(model.description),
            symbols = symbols.mapIndexed { index, symbol -> AvroEnumSymbol(symbol, last = index == symbols.lastIndex) },
            default = model.default,
        )
    }

    private fun record(
        name: String,
        description: String?,
        properties: List<PropertyDefinition>,
    ): AvroRecord {
        requireAvroName("record", name, name)
        return AvroRecord(
            namespace = packageName,
            name = name,
            doc = doc(description),
            fields =
                properties.mapIndexed { index, property ->
                    requireAvroName("field", property.wireName, name)
                    field(property, last = index == properties.lastIndex)
                },
        )
    }

    private fun field(
        property: PropertyDefinition,
        last: Boolean,
    ): AvroField {
        val type = typeMapper.map(property.type)
        val default = property.default?.let(::defaultJson)
        return AvroField(
            name = property.wireName,
            doc = doc(property.description),
            jsonType =
                if (property.required) {
                    type.json()
                } else {
                    AvroType.optional(type, hasDefault = default != null).json()
                },
            last = last,
            defaultJson = default ?: "null".takeUnless { property.required },
        )
    }

    /**
     * A default as Avro default JSON, for scalars and enum symbols. Dates, times and UUIDs are left
     * out: Avro encodes them differently from JSON Schema.
     */
    private fun defaultJson(default: DefaultValue): String? =
        when (default) {
            is DefaultValue.EnumEntry ->
                default.constant.takeIf { it in enumValues[default.enumName].orEmpty() }?.let(::quoted)
            is DefaultValue.StringLiteral -> quoted(default.value)
            is DefaultValue.BooleanLiteral -> default.value.takeIf { it == "true" || it == "false" }
            is DefaultValue.IntegerLiteral -> default.value.takeIf { it.toBigDecimalOrNull() != null }
            is DefaultValue.NumberLiteral -> default.value.takeIf { it.toBigDecimalOrNull() != null }
            is DefaultValue.ParsedScalar -> null
        }

    private fun requireAvroName(
        kind: String,
        value: String,
        schemaName: String,
    ) {
        if (!AVRO_NAME.matches(value)) {
            throw InvalidAvroName(kind = kind, avroName = value, location = "components.schemas.$schemaName")
        }
    }

    private fun EnumModel.values(): List<String> = constants.map { it.jsonValue ?: it.name }

    private fun quoted(text: String): String = "\"${jsonText(text)}\""

    /** A description as one line of Avro `doc` text, quoted for JSON; null without a description. */
    private fun doc(description: String?): String? =
        description
            ?.takeUnless { it.isBlank() }
            ?.replace("\n", " ")
            ?.replace("\r", "")
            ?.trimStart('"', '\'', '|', '>')
            ?.trim()
            ?.let(::jsonText)

    private fun jsonText(text: String): String = String(JsonStringEncoder.getInstance().quoteAsString(text))

    private companion object {
        val AVRO_NAME = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }
}
