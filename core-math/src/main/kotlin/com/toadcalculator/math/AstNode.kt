// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import java.math.BigDecimal

/**
 * Abstract syntax tree produced by [Parser]. Kept intentionally tiny.
 */
sealed class AstNode {
    data class NumberLiteral(val value: BigDecimal) : AstNode()
    data class Constant(val name: String, val value: BigDecimal) : AstNode()
    data class Call(val function: String, val arg: AstNode) : AstNode()

    /** Parenthesized sub-expression; preserves "(x)" syntactic info (e.g. "(10%)" ≠ "10%"). */
    data class Group(val inner: AstNode) : AstNode()

    data class UnaryMinus(val operand: AstNode) : AstNode()
    data class Factorial(val operand: AstNode) : AstNode()

    /**
     * Postfix percent. Standalone: x/100. As the right operand of + / −:
     * A ± A·x/100 (percent-of-total semantics). As an operand of × / ÷ it
     * evaluates to x/100 like anywhere else.
     */
    data class Percent(val operand: AstNode) : AstNode()

    data class Power(val base: AstNode, val exponent: AstNode) : AstNode()

    enum class BinaryKind { ADD, SUB, MUL, DIV, MOD }

    data class Binary(
        val kind: BinaryKind,
        val left: AstNode,
        val right: AstNode,
    ) : AstNode()
}
