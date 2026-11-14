package dev.banking.asyncapi.generator.core.generator.ir

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.schema.isOpenPayload
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.hasMultipleNonNullTypes
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Decides the [TypeRef] of a property schema: which scalar, collection, or generated model
 * represents it in generated source.
 */
class ModelTypeMapper(
    private val context: GeneratorContext,
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
        if (!schema.oneOf.isNullOrEmpty() || !schema.anyOf.isNullOrEmpty()) {
            return named(MapperUtil.toPascalCase(propertyName))
        }
        if (schema.type.getPrimaryType() == "string" && !schema.enum.isNullOrEmpty()) {
            return named(
                schema.title?.takeIf { it.isNotBlank() }
                    ?.let(MapperUtil::toPascalCase)
                    ?: MapperUtil.toPascalCase(propertyName),
            )
        }

        return when (schema.type.getPrimaryType()) {
            "string" -> mapString(schema)
            "integer" -> mapInteger(schema)
            "number" -> mapNumber(schema)
            "boolean" -> TypeRef.Scalar.BOOLEAN
            "array" -> mapArray(propertyName, schema)
            "object" -> mapObject(propertyName, schema) ?: TypeRef.Scalar.ANY
            else -> TypeRef.Scalar.ANY
        }
    }

    /** The generated model a reference points to; marked when it is an open object. */
    fun mapReference(reference: Reference): TypeRef.Named {
        val raw = reference.ref.substringAfterLast("/")
        val target = context.findSchemaByName(raw) ?: reference.model as? Schema
        return named(MapperUtil.toPascalCase(raw), isOpenObject = target?.isOpenPayload() == true)
    }

    private fun named(
        name: String,
        isOpenObject: Boolean = false,
    ): TypeRef.Named = TypeRef.Named(name = name, isModel = isModel(name), isOpenObject = isOpenObject)

    private fun isModel(name: String): Boolean {
        if (name in builtInTypeNames) return false
        val schema = context.findSchemaByName(name) ?: return name.firstOrNull()?.isUpperCase() == true
        return schema.type.getPrimaryType() == "object" || !schema.oneOf.isNullOrEmpty() || !schema.anyOf.isNullOrEmpty()
    }

    private fun mapString(schema: Schema): TypeRef =
        when (schema.format) {
            "uuid" -> TypeRef.Scalar.UUID
            "date-time" -> TypeRef.Scalar.DATE_TIME
            "date" -> TypeRef.Scalar.DATE
            "time" -> TypeRef.Scalar.TIME
            else -> TypeRef.Scalar.STRING
        }

    private fun mapInteger(schema: Schema): TypeRef {
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

    private fun mapNumber(schema: Schema): TypeRef {
        if (schema.multipleOf != null) return TypeRef.Scalar.DECIMAL
        return if (schema.format?.lowercase() == "float") TypeRef.Scalar.FLOAT else TypeRef.Scalar.DOUBLE
    }

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
                    val referencedSchema = context.findSchemaByName(referenceName)
                    val isPlainString =
                        referencedSchema?.type.getPrimaryType() == "string" && referencedSchema?.enum.isNullOrEmpty()
                    if (isPlainString) TypeRef.Scalar.STRING else named(MapperUtil.toPascalCase(referenceName))
                }
                else -> TypeRef.Scalar.ANY
            }
        return TypeRef.ListOf(element)
    }

    private fun mapObject(
        propertyName: String,
        schema: Schema,
    ): TypeRef? {
        check(schema.properties.isNullOrEmpty()) {
            "Type mapping encountered an inline object with properties at property '$propertyName'. " +
                "This schema should have been promoted to a top-level schema reference by InlineSchemaAnalyzer. " +
                "This indicates a bug in the generator pipeline."
        }
        schema.title?.takeIf { it.isNotBlank() }?.let { return named(MapperUtil.toPascalCase(it)) }
        val value =
            when (val additionalProperties = schema.additionalProperties) {
                null -> TypeRef.Scalar.ANY
                is SchemaInterface.BooleanSchema -> if (additionalProperties.value) TypeRef.Scalar.ANY else return null
                is SchemaInterface.SchemaInline -> map(propertyName + "Value", additionalProperties.schema)
                is SchemaInterface.SchemaReference -> mapReference(additionalProperties.reference)
                else -> TypeRef.Scalar.ANY
            }
        return TypeRef.MapOf(value)
    }
}
