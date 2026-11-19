package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.validator.schemas.InstanceEvaluation.Companion.violation
import java.math.BigDecimal
import java.util.regex.Pattern

/** Checks numbers and strings against the Draft 7 keywords that constrain them. */
internal object ScalarInstanceRules {
    /** Checks the number [value] at [path] against the numeric keywords of [schema]. */
    fun evaluateNumber(
        schema: Schema,
        value: Number,
        path: String,
    ): InstanceEvaluation {
        val number = SchemaValueSemantics.decimal(value) ?: return InstanceEvaluation()
        var result = InstanceEvaluation()
        schema.minimum?.let(SchemaValueSemantics::decimal)?.takeIf { number < it }?.let {
            result += violation(path, "is lower than the inclusive minimum $it")
        }
        schema.maximum?.let(SchemaValueSemantics::decimal)?.takeIf { number > it }?.let {
            result += violation(path, "is greater than the inclusive maximum $it")
        }
        schema.exclusiveMinimum?.let(SchemaValueSemantics::decimal)?.takeIf { number <= it }?.let {
            result += violation(path, "is not greater than the exclusive minimum $it")
        }
        schema.exclusiveMaximum?.let(SchemaValueSemantics::decimal)?.takeIf { number >= it }?.let {
            result += violation(path, "is not lower than the exclusive maximum $it")
        }
        schema.multipleOf
            ?.let(SchemaValueSemantics::decimal)
            ?.takeIf { divisor ->
                divisor.signum() > 0 && number.remainder(divisor).compareTo(BigDecimal.ZERO) != 0
            }?.let { divisor ->
                result += violation(path, "is not a multiple of $divisor")
            }
        return result
    }

    /** Checks the string [value] at [path] against the string keywords of [schema]. */
    fun evaluateString(
        schema: Schema,
        value: String,
        path: String,
    ): InstanceEvaluation {
        val length = value.codePointCount(0, value.length).toBigDecimal()
        var result = InstanceEvaluation()
        schema.minLength?.let(SchemaValueSemantics::decimal)?.takeIf { length < it }?.let {
            result += violation(path, "is shorter than minLength $it")
        }
        schema.maxLength?.let(SchemaValueSemantics::decimal)?.takeIf { length > it }?.let {
            result += violation(path, "is longer than maxLength $it")
        }
        schema.pattern?.let { regex ->
            val matches = runCatching { Pattern.compile(regex).matcher(value).find() }.getOrDefault(true)
            if (!matches) result += violation(path, "does not match pattern '$regex'")
        }
        return result
    }
}
