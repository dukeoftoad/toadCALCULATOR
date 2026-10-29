// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FuzzTest {

    private val atoms = listOf(
        "0", "1", "2", "3", "7", "9", "10", "1.5", "0.001", "2.7", "1000000",
        "+", "-", "*", "/", "%", "^", "!", "(", ")", ".", " ", "e", "π",
        "sin(", "cos(", "tan(", "ln(", "sqrt(", "abs(", "exp(", "floor(",
    )

    /**
     * 1000 randomly assembled expressions: the engine must never throw —
     * every input maps to exactly Success or one of the six typed errors.
     */
    @Test(timeout = 120_000)
    fun `random expressions never crash the engine`() {
        val rng = java.util.Random(42L)
        val allowedErrors = setOf(
            MathError.DivisionByZero, MathError.DomainError, MathError.Overflow,
            MathError.Undefined, MathError.UnbalancedParentheses,
        )
        repeat(1000) {
            val len = 1 + rng.nextInt(20)
            val sb = StringBuilder()
            repeat(len) { sb.append(atoms[rng.nextInt(atoms.size)]) }
            val expr = sb.toString()
            val result = evaluate(expr, if (rng.nextBoolean()) AngleMode.DEG else AngleMode.RAD)
            when (result) {
                is MathResult.Success -> {
                    // Success must carry a finite, bounded value.
                    assertThat(result.value.abs().compareTo(Evaluator.LIMIT) <= 0).isTrue()
                }
                is MathResult.Failure -> {
                    val err = result.error
                    if (err is MathError.SyntaxError) {
                        assertThat(err.position >= 0).isTrue()
                    } else {
                        assertThat(allowedErrors).contains(err)
                    }
                }
            }
        }
    }

    /**
     * 1000 syntactically plausible expressions built with balanced parens:
     * exercises the evaluator much harder than pure noise.
     */
    @Test(timeout = 120_000)
    fun `balanced random expressions never crash the engine`() {
        val rng = java.util.Random(1337L)
        val values = listOf("0", "1", "2", "3.5", "10", "0.1", "e", "π", "1e3", "0.5")
        val unaryOps = listOf("-", "", "", "")
        val binaryOps = listOf("+", "-", "*", "/", "%", "^")
        val fns = listOf("", "", "", "sin(", "cos(", "tan(", "sqrt(", "ln(", "log(", "abs(")

        fun value(): String {
            val f = fns[rng.nextInt(fns.size)]
            val v = values[rng.nextInt(values.size)]
            return if (f.endsWith("(")) f + v + ")" else v
        }

        fun term(): String {
            var t = unaryOps[rng.nextInt(unaryOps.size)] + value()
            // optional postfixes
            if (rng.nextInt(6) == 0) t += "!"
            if (rng.nextInt(8) == 0) t += "%"
            return t
        }

        fun expr(depth: Int): String {
            var e = term()
            repeat(rng.nextInt(3) + 1) {
                e += binaryOps[rng.nextInt(binaryOps.size)] + term()
            }
            return if (depth > 0 && rng.nextInt(3) == 0) "(" + e + ")" else e
        }

        repeat(1000) {
            val e = expr(2)
            val result = evaluate(e, if (rng.nextBoolean()) AngleMode.DEG else AngleMode.RAD)
            when (result) {
                is MathResult.Success -> assertThat(result.value).isNotNull()
                is MathResult.Failure -> assertThat(result.error).isNotNull()
            }
        }
    }

    /** Adversarial inputs aimed at the lexer, parser and magnitude guards. */
    @Test(timeout = 60_000)
    fun `adversarial inputs never crash or hang`() {
        val adversarial = listOf(
            "", " ", "(", ")", "(())", "((((((((((1))))))))))", "!", "!!", "%%", "%!",
            "1e", "1e+", "1e-", "1e-99999999999999999999", "1e99999999999999999999",
            "1e-999999999", "1e999999999", "0.0.0.0", "..", "...", "2^.^3",
            "1e999999999^1e999999999", "9!".repeat(3), "5!%2^3",
            "sin()cos()tan()", "ππππππ", "e^e^e^e", "(1+2))((3)",
            "1/0/0/0", "0%0%0", "((((((((((", "))))))))))",
            "1000000000000000000000000000000!", "999999999999999999999^2",
            "0^-999999999999999999999", "0^1e999999999",
            "1e-999999999+1e-999999999", "1e999999999*1e999999999",
            "1e999999999%3", "1e999999999!", "(1e999999999)!",
            "2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2^2",
            "9^9^9^9^9^9^9^9^9^9^9^9",
            "0.00000000000000000000001^0.00000000000000000000001",
            "1/1e-999999999", "1e999999999/1e-999999999",
        )
        for (expr in adversarial) {
            val result = evaluate(expr)
            assertThat(result).isNotNull()
        }
    }
}
