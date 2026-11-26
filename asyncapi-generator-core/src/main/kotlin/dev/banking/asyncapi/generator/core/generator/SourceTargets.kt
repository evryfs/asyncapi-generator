package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.java.JavaTarget
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinTarget
import dev.banking.asyncapi.generator.core.generator.sourcetarget.SourceTarget

/** The [SourceTarget] of every [SourceLanguage]. */
internal object SourceTargets {
    private val targets: Map<SourceLanguage, SourceTarget> =
        listOf(KotlinTarget, JavaTarget).associateBy(SourceTarget::language).also { targets ->
            check(targets.keys == SourceLanguage.entries.toSet()) {
                "Source languages without a target: ${SourceLanguage.entries - targets.keys}"
            }
        }

    /** The target that writes [language]. */
    fun of(language: SourceLanguage): SourceTarget = targets.getValue(language)
}
