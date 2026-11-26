package dev.banking.asyncapi.generator.core.generator.avro.mapper

import dev.banking.asyncapi.generator.core.generator.avro.model.AvroType
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelDefinition
import dev.banking.asyncapi.generator.core.generator.sourcemodel.OpenObjectModel
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef
import dev.banking.asyncapi.generator.core.generator.sourcemodel.UnionModel
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UntypedAvroValue

/**
 * Maps the types of the source model to Avro types.
 *
 * Models are referred to by their full name, `<namespace>.<Name>`, which is the name their own
 * `.avsc` file declares. A union has no schema of its own: a field, array item or map value typed
 * by a union holds the union's members directly.
 *
 * @param packageName the Avro namespace of the generated schemas
 * @param models the models of the contract, to find the members of a union and the open objects
 */
internal class AvroTypeMapper(
    private val packageName: String,
    models: List<ModelDefinition>,
) {
    private val unions = models.filterIsInstance<UnionModel>().associateBy(UnionModel::name)
    private val openObjects = models.filterIsInstance<OpenObjectModel>().map(OpenObjectModel::name).toSet()

    /**
     * The Avro type of [type], the type of the value at [location]. A value that may be anything,
     * such as an open object, fails: Avro has no type for it.
     */
    fun map(
        type: TypeRef,
        location: String,
    ): AvroType =
        when (type) {
            is TypeRef.Scalar -> scalar(type) ?: throw UntypedAvroValue(location)
            is TypeRef.ListOf -> AvroType.ArrayOf(map(type.element, location))
            is TypeRef.MapOf -> AvroType.MapOf(map(type.value, location))
            is TypeRef.Named -> {
                if (type.name in openObjects) throw UntypedAvroValue(location)
                unionOf(type.name) ?: named(type.name)
            }
            is TypeRef.External -> AvroType.Named("${type.packageName}.${type.name}")
        }

    /** The members of the union [name] as an Avro union, or null when [name] is not a union. */
    private fun unionOf(name: String): AvroType? =
        unions[name]?.members?.takeIf { it.isNotEmpty() }?.let { members -> AvroType.Union(members.map(::named)) }

    private fun named(name: String): AvroType = AvroType.Named("$packageName.$name")

    /** The Avro type of a scalar; null for [TypeRef.Scalar.ANY], which may be any value. */
    private fun scalar(type: TypeRef.Scalar): AvroType? =
        when (type) {
            TypeRef.Scalar.ANY -> null
            TypeRef.Scalar.STRING, TypeRef.Scalar.TIME -> AvroType.STRING
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
