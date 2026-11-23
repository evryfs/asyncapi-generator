package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/**
 * One kind of inline-or-reference member, such as a channel or a tag: how to wrap a model or a reference
 * back into the member type, and how to bundle the model's own members.
 *
 * @param I the member type, holding an inline [T] or a reference to one
 * @param T the model type
 * @param inline wraps an inline model into the member type
 * @param reference wraps a reference into the member type
 * @param bundle bundles the members of a model; `null` for a model without members to bundle, which is
 *   kept as it is when inline and copied in when referenced
 */
internal class MemberKind<I : ReferenceOr<T>, T : Any>(
    val inline: (T) -> I,
    val reference: (Reference) -> I,
    val bundle: ((T, BundlingScope) -> T)? = null,
)
