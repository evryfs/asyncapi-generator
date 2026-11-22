package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.diagnostics.ValidationReporter
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport
import dev.banking.asyncapi.generator.core.parser.external.LoadedTarget
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the documents and fragments loaded for references, each on its own.
 *
 * A fragment is validated as the kind of object its reference expects, and its findings name it
 * `External <Kind> '<name>'`.
 *
 * @param context the load the targets belong to
 */
internal class LoadedTargetValidator(
    private val context: AsyncApiContext,
) {
    /**
     * Validates [targets] in order and fails on the first one with errors.
     *
     * @return the warnings of all targets
     */
    fun validate(targets: List<LoadedTarget>): List<ValidationFinding> =
        targets.flatMap { target ->
            val report =
                when (target) {
                    is LoadedTarget.Document -> AsyncApiValidator(context).validate(target.document)
                    is LoadedTarget.Fragment -> validate(target)
                }
            ValidationReporter(context).throwErrors(report)
            report.warnings
        }

    private fun validate(fragment: LoadedTarget.Fragment): ValidationReport {
        val results = ValidationCollector()
        val walk = ValidationWalk(context, results)
        val path = ValidationPath.named("External ${fragment.category.label}", fragment.name)
        when (val model = fragment.model) {
            is SchemaInterface -> walk.schema(model, path)
            is ReferenceOr<*> -> walk.member(model, fragment.category, path)
        }
        walk.drainReferenceTargets()
        return results.report()
    }

    /** The kind's name as written in fragment findings, such as `MessageTrait`. */
    private val ReferenceCategoryKey.label: String
        get() = name.split('_').joinToString("") { word -> word.lowercase().replaceFirstChar(Char::uppercase) }
}
