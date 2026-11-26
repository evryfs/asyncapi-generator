package dev.banking.asyncapi.generator.core.generator.avro.mapper

import dev.banking.asyncapi.generator.core.generator.avro.model.AvroType
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelDefinition
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef
import dev.banking.asyncapi.generator.core.generator.sourcemodel.UnionModel

/**
 * Maps the types of the source model to Avro types.
 *
 * Models are referred to by their full name, `<namespace>.<Name>`, which is the name their own
 * `.avsc` file declares. A field typed by a union holds the union's members directly.
 *
 * @param packageName the Avro namespace of the generated schemas
 * @param models the models of the contract, to find the members of a union
 */
internal class AvroTypeMapper(
    private val packageName: String,
    models: List<ModelDefinition>,
) {
    private val unions = models.filterIsInstance<UnionModel>().associateBy(UnionModel::name)

    /** The Avro type of a record field of [type]. */
    fun fieldType(type: TypeRef): AvroType {
        val union = (type as? TypeRef.Named)?.let { unions[it.name] }
        return if (union != null && union.members.isNotEmpty()) {
            AvroType.Union(union.members.map(::named))
        } else {
            map(type)
        }
    }

    private fun map(type: TypeRef): AvroType =
        when (type) {
            is TypeRef.Scalar -> scalar(type)
            is TypeRef.ListOf -> AvroType.ArrayOf(map(type.element))
            is TypeRef.MapOf -> AvroType.MapOf(map(type.value))
            is TypeRef.Named -> named(type.name)
            is TypeRef.External -> AvroType.Named("${type.packageName}.${type.name}")
        }

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
