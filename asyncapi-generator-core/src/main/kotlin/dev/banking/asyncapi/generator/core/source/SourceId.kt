package dev.banking.asyncapi.generator.core.source

/**
 * Identity of one source document within a load.
 *
 * It is derived from the file name and made unique when two loaded files share a name, so it can
 * prefix the paths of values from different files without ambiguity.
 *
 * @property value the identity as it appears in paths and references
 */
@JvmInline
internal value class SourceId(
    val value: String,
) {
    override fun toString(): String = value
}
