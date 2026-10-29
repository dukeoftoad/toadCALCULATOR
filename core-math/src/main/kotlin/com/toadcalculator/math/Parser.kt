// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import java.math.BigDecimal
import com.toadcalculator.math.Token.Identifier
import com.toadcalculator.math.Token.LParen
import com.toadcalculator.math.Token.Number
import com.toadcalculator.math.Token.Operator
import com.toadcalculator.math.Token.Percent
import com.toadcalculator.math.Token.PercentKind
import com.toadcalculator.math.Token.RParen

/**
 * Recursive-descent parser with precedence climbing.
 *
 * Precedence (lowest → highest):
 *   1. binary + −                        (left)
 *   2. binary * / % (mod), implicit ×    (left)
 *   3. unary + −
 *   4. ^                                 (right; binds tighter than unary minus: -2^2 = -4)
 *   5. postfix ! and %                   (tightest)
 *
 * Implicit multiplication triggers when a complete atom is followed by a
 * Number, Identifier or LParen token; the trigger token itself starts the
 * right operand (it is NOT consumed as an operator). The one exclusion from
 * the spec: a bare number is never implicitly multiplied by another bare
 * number ("10 5" is a syntax error) — detected via [AstNode.NumberLiteral].
 */
internal class Parser(
    private val tokens: List<Token>,
    private val inputLength: Int = 0,
) {
    private var index = 0

    fun parse(): AstNode {
        if (tokens.isEmpty()) throw MathSyntaxException(MathError.SyntaxError(0))
        val node = parseExpression(MIN_PRECEDENCE)
        val rest = peek()
        if (rest != null) {
            if (rest is RParen) throw MathSyntaxException(MathError.UnbalancedParentheses)
            throw MathSyntaxException(MathError.SyntaxError(rest.pos))
        }
        return node
    }

    private fun peek(): Token? = tokens.getOrNull(index)

    private fun consume(): Token = tokens[index++]

    private fun parseExpression(minPrec: Int): AstNode {
        var left = parseUnary()

        loop@ while (true) {
            val t = peek() ?: break
            var opKind: AstNode.BinaryKind? = null
            var prec = -1
            var consumeOperator = true

            when (t) {
                is Operator -> when (t.symbol) {
                    '+', '-' -> { opKind = if (t.symbol == '+') AstNode.BinaryKind.ADD else AstNode.BinaryKind.SUB; prec = 1 }
                    '*', '/' -> { opKind = if (t.symbol == '*') AstNode.BinaryKind.MUL else AstNode.BinaryKind.DIV; prec = 2 }
                    else -> break@loop
                }
                is Percent ->
                    if (t.kind == PercentKind.BINARY_MODULO) {
                        opKind = AstNode.BinaryKind.MOD; prec = 2
                    } else break@loop
                is Number, is Identifier, is LParen -> {
                    // Implicit multiplication; the token starts the right operand.
                    if (t is Number && left is AstNode.NumberLiteral) break@loop // "10 5"
                    opKind = AstNode.BinaryKind.MUL
                    prec = 2
                    consumeOperator = false
                }
                is RParen -> break@loop
            }

            if (prec < minPrec) break
            if (consumeOperator) consume()
            val right = parseExpression(prec + 1)
            left = AstNode.Binary(opKind!!, left, right)
        }
        return left
    }

    private fun parseUnary(): AstNode {
        val t = peek()
        if (t is Operator && t.symbol == '+') {
            consume()
            return parseUnary()
        }
        if (t is Operator && t.symbol == '-') {
            consume()
            return AstNode.UnaryMinus(parseUnary())
        }
        return parsePower()
    }

    private fun parsePower(): AstNode {
        val base = parsePostfix()
        val t = peek()
        if (t is Operator && t.symbol == '^') {
            consume()
            val exponent = parseUnary() // right-assoc; also allows 2^-3
            return AstNode.Power(base, exponent)
        }
        return base
    }

    private fun parsePostfix(): AstNode {
        var node = parsePrimary()
        while (true) {
            val t = peek() ?: break
            if (t is Operator && t.symbol == '!') {
                consume()
                node = AstNode.Factorial(node)
            } else if (t is Percent && t.kind == PercentKind.PERCENT) {
                consume()
                node = AstNode.Percent(node)
            } else {
                break
            }
        }
        return node
    }

    private fun parsePrimary(): AstNode {
        val t = peek() ?: throw MathSyntaxException(MathError.SyntaxError(inputLength))
        return when (t) {
            is Number -> {
                consume()
                AstNode.NumberLiteral(parseNumber(t))
            }

            is Identifier -> {
                consume()
                val lower = t.name.lowercase()
                val normalized = if (lower == "pi") "π" else lower
                val next = peek()
                if (normalized in Functions.NAMES) {
                    if (next is LParen) {
                        consume() // (
                        val arg = parseExpression(MIN_PRECEDENCE)
                        expectRParen()
                        AstNode.Call(normalized, arg)
                    } else {
                        throw MathSyntaxException(MathError.SyntaxError(t.pos))
                    }
                } else {
                    val constant = CONSTANTS[normalized]
                        ?: throw MathSyntaxException(MathError.SyntaxError(t.pos))
                    AstNode.Constant(normalized, constant)
                }
            }

            is LParen -> {
                consume()
                val inner = parseExpression(MIN_PRECEDENCE)
                expectRParen()
                AstNode.Group(inner)
            }

            is RParen -> throw MathSyntaxException(MathError.UnbalancedParentheses)

            is Percent -> throw MathSyntaxException(MathError.SyntaxError(t.pos))

            is Operator -> throw MathSyntaxException(MathError.SyntaxError(t.pos))
        }
    }

    private fun expectRParen() {
        when (peek()) {
            is RParen -> consume()
            else -> throw MathSyntaxException(MathError.UnbalancedParentheses)
        }
    }

    private fun parseNumber(token: Token.Number): BigDecimal = try {
        BigDecimal(token.text)
    } catch (_: NumberFormatException) {
        // Covers exponents beyond BigDecimal's scale range, e.g. "1e-99999999999999999999".
        throw MathSyntaxException(MathError.SyntaxError(token.pos))
    }

    companion object {
        private const val MIN_PRECEDENCE = 1

        val CONSTANTS: Map<String, BigDecimal> = mapOf(
            "π" to BigDecimal(Math.PI.toString()),
            "e" to BigDecimal(Math.E.toString()),
        )
    }
}
