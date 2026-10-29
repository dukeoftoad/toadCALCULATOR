// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

/**
 * Typed evaluation errors.
 *
 * Semantics:
 * - DivisionByZero: x/0 or x%0 with x != 0.
 * - Undefined: 0/0 and 0%0 (indeterminate forms).
 * - DomainError: function applied outside its domain (sqrt(-1), ln(0),
 *   asin(2), tan(90°), (-2)^0.5, factorial of a negative or non-integer, ...).
 * - Overflow: |result| would exceed 1e6144 (the IEEE 754 decimal128 range
 *   this engine treats as its upper bound), or a double-based function
 *   produced ±Infinity.
 * - SyntaxError: malformed input at [position] (0-based index into the input).
 * - UnbalancedParentheses: missing "(" or ")".
 */
sealed class MathError {
    data object DivisionByZero : MathError()
    data object Undefined : MathError()
    data object DomainError : MathError()
    data object Overflow : MathError()
    data class SyntaxError(val position: Int) : MathError()
    data object UnbalancedParentheses : MathError()
}

/** Thrown internally by the lexer/parser; converted to [MathError.SyntaxError] at the boundary. */
internal class MathSyntaxException(val error: MathError) : Exception(error.toString())

/** Thrown internally by the evaluator; converted to the wrapped [MathError] at the boundary. */
internal class MathEvalException(val error: MathError) : Exception(error.toString())
