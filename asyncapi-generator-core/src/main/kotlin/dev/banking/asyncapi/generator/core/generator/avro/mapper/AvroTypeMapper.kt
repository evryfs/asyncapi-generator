package dev.banking.asyncapi.generator.core.generator.avro.mapper

import dev.banking.asyncapi.generator.core.generator.avro.model.AvroType
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelDefinition
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef
import dev.banking.asyncapi.generator.core.generator.sourcemodel.UnionModel

/**
 * Maps the types of the source model to Avro types.
 *
 * Models are referred to by their full name, `<namespace>.<Name>`, which is the name their own
 * `.avsc` file declares. A union has no schema of its own: a field, array item or map value typed
 * by a union holds the union's members directly.
 *
 * @param packageName the Avro namespace of the generated schemas
 * @param models the models of the contract, to find the members of a union
 */
internal class AvroTypeMapper(
    private val packageName: String,
    models: List<ModelDefinition>,
) {
    private val unions = models.filterIsInstance<UnionModel>().associateBy(UnionModel::name)

    /** The Avro type of [type]. */
    fun map(type: TypeRef): AvroType =
        when (type) {
            is TypeRef.Scalar -> scalar(type)
            is TypeRef.ListOf -> AvroType.ArrayOf(map(type.element))
            is TypeRef.MapOf -> AvroType.MapOf(map(type.value))
            is TypeRef.Named -> unionOf(type.name) ?: named(type.name)
            is TypeRef.External -> AvroType.Named("${type.packageName}.${type.name}")
        }

    /** The members of the union [name] as an Avro union, or null when [name] is not a union. */
    private fun unionOf(name: String): AvroType? =
        unions[name]?.members?.takeIf { it.isNotEmpty() }?.let { members -> AvroType.Union(members.map(::named)) }

    private fun named(name: String): AvroType = AvroType.Named("$packageName.$name")

    private fun scalar(type: TypeRef.Scalar): AvroType =
        when (type) {
            TypeRef.Scalar.STRING, TypeRef.Scalar.TIME, TypeRef.Scalar.ANY -> AvroType.STRING
            TypeRef.Scalar.UUID -> AvroType.Logical("string", "uuid")
            TypeRef.Scalar.DATE -> AvroType.Logical("int", "date")
            TypeRef.Scalar.DATE_TIME -> AvroType.Logical("long", "timestamp-millis")
            TypeRef.Scalar.INT32 -> AvroType.Primitive("int")
            TypeRef.Scalar.INT64 -> AvroType.Primitive("long")
            // Always double: narrowing an existing double field to float breaks registered schemas.
            TypeRef.Scalar.FLOAT, TypeRef.Scalar.DOUBLE, TypeRef.Scalar.DECIMAL -> AvroType.Primitive("double")
            TypeRef.Scalar.BOOLEAN -> AvroType.Primitive("boolean")
        }
}
