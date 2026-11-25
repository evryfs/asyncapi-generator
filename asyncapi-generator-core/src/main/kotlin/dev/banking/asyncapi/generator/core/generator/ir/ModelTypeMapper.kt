package dev.banking.asyncapi.generator.core.generator.ir

import dev.banking.asyncapi.generator.core.generator.schema.generatedSchemaName
import dev.banking.asyncapi.generator.core.generator.schema.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.schema.hasMultipleNonNullTypes
import dev.banking.asyncapi.generator.core.generator.schema.isInlineUnion
import dev.banking.asyncapi.generator.core.generator.schema.isOpenPayload
import dev.banking.asyncapi.generator.core.generator.schema.isScalarAlias
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Decides the [TypeRef] of a property schema: which scalar, collection, or generated model
 * represents it in generated source.
 */
internal class ModelTypeMapper(
    private val schemas: Map<String, Schema>,
) {
    /**
     * The type of an inline [schema] of [propertyName]. Inline enums and unions are named after
     * their title or the property, as the analyzer promoted them to models of that name.
     */
    fun map(
        propertyName: String,
        schema: Schema?,
    ): TypeRef {
        if (schema == null) return TypeRef.Scalar.ANY
        if (schema.type.hasMultipleNonNullTypes()) return TypeRef.Scalar.ANY
        if (schema.isInlineUnion() || (schema.type.getPrimaryType() == "string" && !schema.enum.isNullOrEmpty())) {
            return named(
                schema.title
                    ?.takeIf { it.isNotBlank() }
                    ?.let(NameCase::toPascalCase)
                    ?: NameCase.toPascalCase(propertyName),
            )
        }

        return scalarType(schema) ?: when (schema.type.getPrimaryType()) {
            "array" -> mapArray(propertyName, schema)
            "object" -> mapObject(propertyName, schema) ?: TypeRef.Scalar.ANY
            else -> TypeRef.Scalar.ANY
        }
    }

    /**
     * The type a reference stands for. A named scalar or array schema generates no type of its own,
     * so a reference to it becomes that scalar or list; anything else is the generated model.
     */
    fun resolveReference(
        propertyName: String,
        reference: Reference,
    ): TypeRef {
        val raw = reference.ref.substringAfterLast("/")
        val target = schemas[raw] ?: reference.model as? Schema
        if (target == null || !target.isAlias() || !resolvingAliases.add(raw)) return mapReference(reference)
        return try {
            map(propertyName, target)
        } finally {
            resolvingAliases.remove(raw)
        }
    }

    private val resolvingAliases = mutableSetOf<String>()

    companion object {
        /**
         * The scalar a string, integer, number or boolean schema stands for; `null` for any other schema.
         *
         * A string's `format` chooses a date, time or UUID type, a number is decimal when it has a
         * `multipleOf` and otherwise a float or double by its `format`.
         */
        fun scalarType(schema: Schema): TypeRef.Scalar? =
            when (schema.type.getPrimaryType()) {
                "string" -> stringType(schema)
                "integer" -> integerType(schema)
                "number" -> numberType(schema)
                "boolean" -> TypeRef.Scalar.BOOLEAN
                else -> null
            }

        /** The width of an integer schema: 64-bit by `format: int64` or when its bounds leave the 32-bit range. */
        fun integerType(schema: Schema): TypeRef.Scalar {
            when (schema.format?.lowercase()) {
                "int64" -> return TypeRef.Scalar.INT64
                "int32" -> return TypeRef.Scalar.INT32
            }
            val minimum = schema.minimum
            val maximum = schema.maximum
            val minimumFits = minimum == null || minimum.toDouble() >= Int.MIN_VALUE.toDouble()
            val maximumFits = maximum == null || maximum.toDouble() <= Int.MAX_VALUE.toDouble()
            return if (minimumFits && maximumFits) TypeRef.Scalar.INT32 else TypeRef.Scalar.INT64
        }

        private fun stringType(schema: Schema): TypeRef.Scalar =
            when (schema.format) {
                "uuid" -> TypeRef.Scalar.UUID
                "date-time" -> TypeRef.Scalar.DATE_TIME
                "date" -> TypeRef.Scalar.DATE
                "time" -> TypeRef.Scalar.TIME
                else -> TypeRef.Scalar.STRING
            }

        private fun numberType(schema: Schema): TypeRef.Scalar =
            when {
                schema.multipleOf != null -> TypeRef.Scalar.DECIMAL
                schema.format?.lowercase() == "float" -> TypeRef.Scalar.FLOAT
                else -> TypeRef.Scalar.DOUBLE
            }
    }

    private fun Schema.isAlias(): Boolean =
        isScalarAlias() || (type.getPrimaryType() == "array" && enum.isNullOrEmpty())

    /** The generated model a reference points to; marked when it is an open object. */
    fun mapReference(reference: Reference): TypeRef.Named {
        val raw = reference.ref.substringAfterLast("/")
        val target = schemas[raw] ?: reference.model as? Schema
        return named(reference.generatedSchemaName(), isOpenObject = target?.isOpenPayload() == true)
    }

    private fun named(
        name: String,
        isOpenObject: Boolean = false,
    ): TypeRef.Named = TypeRef.Named(name = name, isModel = isModel(name), isOpenObject = isOpenObject)

    /** Whether [name] is an object or union model; a name without a schema is assumed to be one. */
    private fun isModel(name: String): Boolean =
        schemas[name]?.let { schema -> schema.type.getPrimaryType() == "object" || schema.isInlineUnion() } ?: true

    private fun mapArray(
        propertyName: String,
        schema: Schema,
    ): TypeRef {
        val element =
            when (val items = schema.items) {
                null -> TypeRef.Scalar.ANY
                is SchemaInterface.SchemaInline -> map(propertyName, items.schema)
                is SchemaInterface.SchemaReference -> {
                    val referenceName = items.reference.ref.substringAfterLast("/")
                    val referencedSchema = schemas[referenceName] ?: items.reference.model as? Schema
                    val isPlainString =
                        referencedSchema?.type.getPrimaryType() == "string" && referencedSchema?.enum.isNullOrEmpty()
                    when {
                        isPlainString -> TypeRef.Scalar.STRING
                        referencedSchema?.isAlias() == true -> resolveReference(propertyName, items.reference)
                        else -> named(NameCase.toPascalCase(referenceName))
                    }
                }
                else -> TypeRef.Scalar.ANY
            }
        return TypeRef.ListOf(element)
    }

    /**
     * An inline object without properties is a map of its additional properties, also when it has a
     * title: only objects with properties are promoted to models named after their title.
     */
    private fun mapObject(
        propertyName: String,
        schema: Schema,
    ): TypeRef? {
        check(schema.properties.isNullOrEmpty()) {
            "Type mapping encountered an inline object with properties at property '$propertyName'. " +
                "This schema should have been promoted to a top-level schema reference by InlineSchemaAnalyzer. " +
                "This indicates a bug in the generator pipeline."
        }
        val value =
            when (val additionalProperties = schema.additionalProperties) {
                null -> TypeRef.Scalar.ANY
                is SchemaInterface.BooleanSchema -> if (additionalProperties.value) TypeRef.Scalar.ANY else return null
                is SchemaInterface.SchemaInline -> map(propertyName + "Value", additionalProperties.schema)
                is SchemaInterface.SchemaReference ->
                    resolveReference(
                        propertyName + "Value",
                        additionalProperties.reference,
                    )
                else -> TypeRef.Scalar.ANY
            }
        return TypeRef.MapOf(value)
    }
}
