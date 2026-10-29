// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

/**
 * Lexical tokens. [pos] is the 0-based character index in the source input.
 *
 * '%' tokens are classified at lex time:
 * - BINARY_MODULO when the next non-whitespace character can start an operand
 *   (digit, '.', letter, 'π' or '('): "10 % 3", "10%3" → modulo.
 * - PERCENT when followed by an operator, ')' or end of input: "200+10%", "50%".
 */
sealed class Token {
    abstract val pos: Int

    data class Number(val text: String, override val pos: Int) : Token()
    data class Identifier(val name: String, override val pos: Int) : Token()
    data class Operator(val symbol: Char, override val pos: Int) : Token()
    data class LParen(override val pos: Int) : Token()
    data class RParen(override val pos: Int) : Token()

    enum class PercentKind { BINARY_MODULO, PERCENT }

    data class Percent(override val pos: Int, val kind: PercentKind) : Token()
}
