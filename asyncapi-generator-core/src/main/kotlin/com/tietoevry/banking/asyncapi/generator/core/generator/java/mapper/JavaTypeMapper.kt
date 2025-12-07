package com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.context.GeneratorContext
import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

class JavaTypeMapper(val context: GeneratorContext) {

    private val mappers: List<TypeMapper> = listOf(
        PolymorphicMapper(),
        EnumMapper(),
        StringMapper(),
        NumericMapper(),
        BooleanMapper(),
        ArrayMapper(context),
        ObjectMapper()
    )

    fun mapJavaType(propertyName: String, schema: Schema?): String {
        if (schema == null) return "Object"

        for (mapper in mappers) {
            val mappedType = mapper.map(schema, propertyName, this)
            if (mappedType != null) {
                return mappedType
            }
        }
        return "Object"
    }

    fun typeNameFromRef(reference: Reference): String {
        val raw = reference.ref.substringAfterLast("/")
        return MapperUtil.toPascalCase(raw)
    }
}
