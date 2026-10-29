// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import com.toadcalculator.math.Token.Identifier
import com.toadcalculator.math.Token.LParen
import com.toadcalculator.math.Token.Number
import com.toadcalculator.math.Token.Operator
import com.toadcalculator.math.Token.Percent
import com.toadcalculator.math.Token.PercentKind
import com.toadcalculator.math.Token.RParen

/**
 * Hand-written lexer. Produces the full token list up-front so the parser can
 * do a single-token lookahead cheaply and report absolute error positions.
 *
 * Accepted number syntax: `digits[.digits][e[+-]digits]` or `.digits...`.
 * `e`/`E` is part of the number only when immediately followed by a digit or a
 * sign+digit; otherwise it is Euler's constant ("2e" → 2·e, "2e3" → 2000).
 */
internal class Lexer(private val input: String) {

    fun lex(): List<Token> {
        val tokens = ArrayList<Token>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++

                c.isDigit() || c == '.' -> {
                    val start = i
                    val scanned = scanNumber(i)
                        ?: throw MathSyntaxException(MathError.SyntaxError(start))
                    tokens += Number(scanned.first, start)
                    i = scanned.second
                }

                c.isLetter() || c == 'π' -> {
                    val start = i
                    val name = scanIdentifier(i)
                    tokens += Identifier(name, start)
                    i += name.length
                }

                c == '(' -> { tokens += LParen(i); i++ }
                c == ')' -> { tokens += RParen(i); i++ }

                c == '%' -> {
                    tokens += Percent(i, percentKind(i))
                    i++
                }

                c in "+-*/^!" -> { tokens += Operator(c, i); i++ }
                c == '×' -> { tokens += Operator('*', i); i++ }
                c == '÷' -> { tokens += Operator('/', i); i++ }
                c == '−' || c == '–' -> { tokens += Operator('-', i); i++ }

                else -> throw MathSyntaxException(MathError.SyntaxError(i))
            }
        }
        return tokens
    }

    /**
     * Returns the normalized number text and the index of the first character
     * after the number, or null when there is no digit at all (a bare '.').
     */
    private fun scanNumber(start: Int): Pair<String, Int>? {
        var i = start
        var sawDigit = false
        var sawDot = false
        val sb = StringBuilder()

        while (i < input.length) {
            val c = input[i]
            when {
                c.isDigit() -> { sb.append(c); sawDigit = true; i++ }

                c == '.' && !sawDot -> { sb.append(c); sawDot = true; i++ }

                (c == 'e' || c == 'E') && sawDigit && i + 1 < input.length &&
                    (input[i + 1].isDigit() ||
                        ((input[i + 1] == '+' || input[i + 1] == '-') &&
                            i + 2 < input.length && input[i + 2].isDigit())) -> {
                    // Drop a trailing '.' before the exponent ("1.e3" → "1E3").
                    if (sb.isNotEmpty() && sb.last() == '.') sb.setLength(sb.length - 1)
                    sb.append('E')
                    i++
                    if (i < input.length && (input[i] == '+' || input[i] == '-')) {
                        sb.append(input[i]); i++
                    }
                    while (i < input.length && input[i].isDigit()) { sb.append(input[i]); i++ }
                    return sb.toString() to i
                }

                else -> break
            }
        }
        if (!sawDigit) return null // bare '.'
        // Strip a lone trailing dot ("5." → "5").
        if (sb.isNotEmpty() && sb.last() == '.') sb.setLength(sb.length - 1)
        return sb.toString() to i
    }

    private fun scanIdentifier(start: Int): String {
        if (input[start] == 'π') return "π"
        var i = start
        while (i < input.length && input[i].isLetter()) i++
        // "log2" is one function token ("log" alone is a different function).
        if (i == start + 3 && input.regionMatches(start, "log", 0, 3) &&
            i < input.length && input[i] == '2'
        ) i++
        return input.substring(start, i)
    }

    private fun percentKind(at: Int): PercentKind {
        var j = at + 1
        while (j < input.length && input[j].isWhitespace()) j++
        if (j >= input.length) return PercentKind.PERCENT
        val c = input[j]
        return if (c.isDigit() || c == '.' || c.isLetter() || c == 'π' || c == '(') {
            PercentKind.BINARY_MODULO
        } else {
            PercentKind.PERCENT
        }
    }
}
