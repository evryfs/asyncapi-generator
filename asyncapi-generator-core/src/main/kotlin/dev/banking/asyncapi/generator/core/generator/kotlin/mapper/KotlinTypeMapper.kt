package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.hasMultipleNonNullTypes
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema

class KotlinTypeMapper(
    val context: GeneratorContext,
) {

    private val mappers: List<TypeMapper> = listOf(
        PolymorphicMapper(),
        EnumMapper(),
        StringMapper(),
        NumericMapper(),
        BooleanMapper(),
        ArrayMapper(context), // Pass context to ArrayMapper
        ObjectMapper()
    )

    fun mapKotlinType(propertyName: String, schema: Schema?): String {
        if (schema == null) return "Any"
        // Strict JSON Schema: if multiple non-null types, fall back to Any
        if (schema.type.hasMultipleNonNullTypes()) return "Any"
        for (mapper in mappers) {
            val mappedType = mapper.map(schema, propertyName, this)
            if (mappedType != null) {
                return mappedType
            }
        }
        return "Any"
    }

    fun typeNameFromRef(reference: Reference): String {
        val raw = reference.ref.substringAfterLast("/")
        return MapperUtil.toPascalCase(raw)
    }
}


