package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.sourcemodel.SourceNaming

/**
 * Avro keeps the contract's names: a field is named like its property and an enum symbol like its
 * value. Names that Avro rejects fail when the schemas are built, instead of being renamed.
 */
internal object AvroNaming : SourceNaming {
    override fun propertyIdentifier(wireName: String): String = wireName

    override fun enumConstant(value: String): String = value
}
