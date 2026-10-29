// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.ui.calculator

/**
 * Keypad keys. [text] is the exact string inserted into the expression
 * (ASCII operators: the display renders × ÷ − glyphs but the engine parses
 * ASCII, which keeps history entries re-parseable).
 */
sealed class KeypadKey(val text: String, val display: String = text) {
    data class Digit(val d: Char) : KeypadKey(d.toString())
    data object Dot : KeypadKey(".")
    data object Plus : KeypadKey("+")
    data object Minus : KeypadKey("-")
    data object Mul : KeypadKey("*", "×")
    data object Div : KeypadKey("/", "÷")
    data object LParen : KeypadKey("(")
    data object RParen : KeypadKey(")")
    data object Percent : KeypadKey("%")

    // Non-inserting keys.
    data object Equals : KeypadKey("=")
    data object Backspace : KeypadKey("backspace", "⌫")
    data object Clear : KeypadKey("clear", "AC")
}

/**
 * Token-aware backspace. Deletes one *token* ending at [cursor]:
 * - a whole number run (digits, dots, and scientific-notation E±xx parts),
 * - a whole identifier run (function name or constant; "sin(" disappears as one),
 * - one character for anything else (operators, ')').
 *
 * Returns the new text and cursor. Pure function; unit-tested.
 */
fun backspaceToken(text: String, cursor: Int): Pair<String, Int> {
    if (cursor <= 0 || text.isEmpty()) return text to cursor.coerceIn(0, text.length)
    val end = cursor.coerceAtMost(text.length)
    var start = end

    val isDigitOrDot = { c: Char -> c.isDigit() || c == '.' }

    // Number run (works backwards through digits/dots, then an optional E±xx tail).
    if (isDigitOrDot(text[end - 1])) {
        while (start > 0 && isDigitOrDot(text[start - 1])) start--
        // Scientific tail: 1E5, 1.5E-3 → the whole "1.5E-3" is ONE token. The
        // E must directly follow a digit, so subtraction signs are never consumed.
        var j = start
        if (j > 0 && (text[j - 1] == '+' || text[j - 1] == '-')) j--
        if (j > 0 && (text[j - 1] == 'E' || text[j - 1] == 'e')) {
            j--
            if (j > 0 && text[j - 1].isDigit()) {
                start = j
                if (start > 0 && (text[start - 1] == '+' || text[start - 1] == '-')) {
                    if (start - 1 > 0 && text[start - 2].isDigit()) start--
                }
                while (start > 0 && isDigitOrDot(text[start - 1])) start--
            }
        }
        return text.removeRange(start, end) to start
    }

    // Identifier run (function name / constant). Includes a directly attached
    // '(' so "sin(" goes away in one press.
    val c = text[end - 1]
    if (c.isLetter() || c == 'π') {
        while (start > 0 && (text[start - 1].isLetter() || text[start - 1] == 'π')) start--
        if (start < end && end < text.length && text[end] == '(') {
            return text.removeRange(start, end + 1) to start
        }
        return text.removeRange(start, end) to start
    }

    // Cursor sits right after '(' that is part of a function call → delete name+paren.
    if (c == '(') {
        var j = end - 1
        while (j > 0 && (text[j - 1].isLetter() || text[j - 1] == 'π')) j--
        if (j < end - 1 && (text[j].isLetter() || text[j] == 'π')) {
            return text.removeRange(j, end) to j
        }
    }

    // Everything else: one character.
    return text.removeRange(end - 1, end) to end - 1
}

/** True when the number segment ending at [cursor] already contains a dot. */
fun currentNumberHasDot(text: String, cursor: Int): Boolean {
    var i = cursor
    while (i > 0) {
        val c = text[i - 1]
        if (c == '.') return true
        if (!c.isDigit()) return false
        i--
    }
    return false
}
