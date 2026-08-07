package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import java.math.BigDecimal

class SchemaMerger {
    fun merge(base: Schema, override: Schema): Schema {
        val baseProps = base.properties ?: emptyMap()
        val overrideProps = override.properties ?: emptyMap()
        val allPropKeys = baseProps.keys + overrideProps.keys

        val mergedProperties =
            allPropKeys
                .associateWith { key ->
                    mergeSchemaInterface(baseProps[key], overrideProps[key])!!
                }.ifEmpty { null }
        val lowerBound = strictestLowerBound(base, override)
        val upperBound = strictestUpperBound(base, override)
        val overrideHasDefault = override.defaultSet || override.default != null
        val overrideHasConst = override.constSet || override.const != null

        return override.copy(
            title = override.title ?: base.title,
            type = override.type ?: base.type,
            enum = override.enum ?: base.enum,
            format = override.format ?: base.format,
            description = override.description ?: base.description,
            default = if (overrideHasDefault) override.default else base.default,
            defaultSet = if (overrideHasDefault) override.defaultSet else base.defaultSet,
            const = if (overrideHasConst) override.const else base.const,
            constSet = if (overrideHasConst) override.constSet else base.constSet,
            readOnly = override.readOnly ?: base.readOnly,
            writeOnly = override.writeOnly ?: base.writeOnly,
            multipleOf = override.multipleOf ?: base.multipleOf,
            minimum = lowerBound?.value?.takeUnless { lowerBound.exclusive },
            exclusiveMinimum = lowerBound?.value?.takeIf { lowerBound.exclusive },
            maximum = upperBound?.value?.takeUnless { upperBound.exclusive },
            exclusiveMaximum = upperBound?.value?.takeIf { upperBound.exclusive },
            minLength = max(base.minLength, override.minLength),
            maxLength = min(base.maxLength, override.maxLength),
            pattern = override.pattern ?: base.pattern,
            items = mergeSchemaInterface(base.items, override.items),
            additionalProperties = mergeSchemaInterface(base.additionalProperties, override.additionalProperties),
            properties = mergedProperties,
            required = (base.required.orEmpty() + override.required.orEmpty()).distinct().ifEmpty { null },
            oneOf = override.oneOf ?: base.oneOf,
            anyOf = override.anyOf ?: base.anyOf,
            discriminator = override.discriminator ?: base.discriminator,
            allOf = null,
        )
    }

    private fun mergeSchemaInterface(
        base: SchemaInterface?,
        override: SchemaInterface?,
    ): SchemaInterface? =
        if (base is SchemaInterface.SchemaInline && override is SchemaInterface.SchemaInline) {
            SchemaInterface.SchemaInline(merge(base.schema, override.schema))
        } else {
            override ?: base
        }

    private fun strictestLowerBound(
        base: Schema,
        override: Schema,
    ): NumericBound? =
        listOfNotNull(
            base.minimum?.let { NumericBound(it, exclusive = false) },
            base.exclusiveMinimum?.let { NumericBound(it, exclusive = true) },
            override.minimum?.let { NumericBound(it, exclusive = false) },
            override.exclusiveMinimum?.let { NumericBound(it, exclusive = true) },
        ).maxWithOrNull(
            compareBy<NumericBound> { bound -> bound.decimalValue }
                .thenBy { bound -> bound.exclusive },
        )

    private fun strictestUpperBound(
        base: Schema,
        override: Schema,
    ): NumericBound? =
        listOfNotNull(
            base.maximum?.let { NumericBound(it, exclusive = false) },
            base.exclusiveMaximum?.let { NumericBound(it, exclusive = true) },
            override.maximum?.let { NumericBound(it, exclusive = false) },
            override.exclusiveMaximum?.let { NumericBound(it, exclusive = true) },
        ).minWithOrNull(
            compareBy<NumericBound> { bound -> bound.decimalValue }
                .thenByDescending { bound -> bound.exclusive },
        )

    private fun max(
        first: Number?,
        second: Number?,
    ): Number? =
        listOfNotNull(first, second)
            .maxByOrNull { value -> BigDecimal(value.toString()) }

    private fun min(
        first: Number?,
        second: Number?,
    ): Number? =
        listOfNotNull(first, second)
            .minByOrNull { value -> BigDecimal(value.toString()) }

    private data class NumericBound(
        val value: Number,
        val exclusive: Boolean,
    ) {
        val decimalValue: BigDecimal = BigDecimal(value.toString())
    }
}
