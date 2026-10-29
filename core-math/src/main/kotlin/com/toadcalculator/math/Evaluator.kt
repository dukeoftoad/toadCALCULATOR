// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

/**
 * BigDecimal evaluator. All arithmetic runs in [MathContext.DECIMAL64]
 * so 0.1 + 0.2 formats as 0.3. Doubles are used only inside [Functions].
 *
 * Overflow bound: |value| > 1e6144 (IEEE decimal128 emax) → MathError.Overflow.
 * This applies to literals and intermediates alike, which also protects the
 * engine from pathological input scales (e.g. 1e999999999 + 1e-999999999).
 */
internal class Evaluator(private val angleMode: AngleMode) {

    fun eval(node: AstNode): BigDecimal = when (node) {
        is AstNode.NumberLiteral -> bounded(node.value)

        is AstNode.Constant -> node.value

        is AstNode.Call -> Functions.apply(node.function, eval(node.arg), angleMode)

        is AstNode.Group -> eval(node.inner)

        is AstNode.UnaryMinus -> eval(node.operand).negate(MC)

        is AstNode.Factorial -> factorial(eval(node.operand))

        is AstNode.Percent -> eval(node.operand).divide(HUNDRED, MC)

        is AstNode.Power -> power(eval(node.base), eval(node.exponent))

        is AstNode.Binary -> binary(node)
    }

    private fun bounded(v: BigDecimal): BigDecimal {
        if (v.abs() > LIMIT) throw MathEvalException(MathError.Overflow)
        return v
    }

    private fun binary(node: AstNode.Binary): BigDecimal {
        val l = eval(node.left)
        val r = node.right

        // Percent-of-total semantics: A ± B% = A ± A·B/100. Applies only when
        // the operand is *syntactically* a percent expression; parenthesized
        // or negated percents behave as plain x/100.
        if (node.kind == AstNode.BinaryKind.ADD && r is AstNode.Percent) {
            val p = eval(r.operand)
            return checked(l, l.add(l.multiply(p, MC).divide(HUNDRED, MC)))
        }
        if (node.kind == AstNode.BinaryKind.SUB && r is AstNode.Percent) {
            val p = eval(r.operand)
            return checked(l, l.subtract(l.multiply(p, MC).divide(HUNDRED, MC)))
        }

        val rv = eval(r)
        return when (node.kind) {
            AstNode.BinaryKind.ADD -> checked(l, l.add(rv, MC))
            AstNode.BinaryKind.SUB -> checked(l, l.subtract(rv, MC))
            AstNode.BinaryKind.MUL -> checked(l, l.multiply(rv, MC))
            AstNode.BinaryKind.DIV -> {
                if (rv.signum() == 0) {
                    throw MathEvalException(if (l.signum() == 0) MathError.Undefined else MathError.DivisionByZero)
                }
                checked(l, l.divide(rv, MC))
            }
            AstNode.BinaryKind.MOD -> {
                if (rv.signum() == 0) {
                    throw MathEvalException(if (l.signum() == 0) MathError.Undefined else MathError.DivisionByZero)
                }
                // remainder() can expand digits; refuse extreme magnitudes first.
                if (l.abs() > LIMIT || rv.abs() > LIMIT) throw MathEvalException(MathError.Overflow)
                l.remainder(rv)
            }
        }
    }

    /** Bounds-checks the result; [l] is only used to keep call sites readable. */
    private fun checked(l: BigDecimal, result: BigDecimal): BigDecimal {
        if (result.abs() > LIMIT) throw MathEvalException(MathError.Overflow)
        return result
    }

    private fun power(base: BigDecimal, exponent: BigDecimal): BigDecimal {
        if (base.abs() > LIMIT) throw MathEvalException(MathError.Overflow)
        val expDouble = exponent.toDouble()

        // Normal path: exponent representable as int.
        if (!expDouble.isInfinite() && abs(expDouble) <= Int.MAX_VALUE) {
            val isIntegral = exponent.stripTrailingZeros().scale() <= 0
            return if (isIntegral) {
                val n = exponent.intValueExact()
                if (base.signum() == 0) {
                    if (n < 0) throw MathEvalException(MathError.DivisionByZero)
                    return if (n == 0) BigDecimal.ONE else BigDecimal.ZERO
                }
                checked(base, base.pow(n, MC))
            } else {
                if (base.signum() < 0) throw MathEvalException(MathError.DomainError)
                val d = Math.pow(base.toDouble(), expDouble)
                if (d.isNaN()) throw MathEvalException(MathError.DomainError)
                if (d.isInfinite()) throw MathEvalException(MathError.Overflow)
                checked(base, BigDecimal.valueOf(d))
            }
        }

        // Astronomical exponent (|exp| > 2^31). Only reachable via literals
        // like 10^1e999999999; defined for completeness, documented in WORKLOG.
        val hugePositive = exponent.signum() > 0
        if (base.signum() == 0) {
            if (hugePositive) return BigDecimal.ZERO
            throw MathEvalException(MathError.DivisionByZero)
        }
        val integral = exponent.stripTrailingZeros().scale() <= 0
        if (base.abs().compareTo(BigDecimal.ONE) == 0) {
            if (!integral) throw MathEvalException(MathError.DomainError)
            val odd = exponent.toBigInteger().testBit(0)
            return if (base.signum() < 0 && odd) BigDecimal.ONE.negate() else BigDecimal.ONE
        }
        val baseGreaterThanOne = base.abs() > BigDecimal.ONE
        if (base.signum() < 0 && !integral) throw MathEvalException(MathError.DomainError)
        if ((hugePositive && baseGreaterThanOne) || (!hugePositive && !baseGreaterThanOne)) {
            throw MathEvalException(MathError.Overflow)
        }
        // Otherwise the result underflows toward 0; the engine has no
        // underflow concept, so it is exactly 0.
        return BigDecimal.ZERO
    }

    private fun factorial(x: BigDecimal): BigDecimal {
        if (x.abs() > LIMIT) throw MathEvalException(MathError.Overflow)
        val integral = x.signum() >= 0 && x.stripTrailingZeros().scale() <= 0
        if (!integral) throw MathEvalException(MathError.DomainError)
        val n = try {
            x.intValueExact()
        } catch (_: ArithmeticException) {
            throw MathEvalException(MathError.Overflow)
        }
        // The per-iteration bound below guarantees termination after ~2150
        // iterations even for absurd n, because n! passes 1e6144 by n ≈ 2140.
        var acc = BigDecimal.ONE
        for (i in 2..n) {
            acc = acc.multiply(BigDecimal(i), MC)
            if (acc > LIMIT) throw MathEvalException(MathError.Overflow)
        }
        return acc
    }

    companion object {
        private val MC = MathContext.DECIMAL64
        private val HUNDRED = BigDecimal(100)

        /** Upper magnitude bound: IEEE 754 decimal128 emax = 6144. */
        val LIMIT: BigDecimal = BigDecimal("1E+6144")
    }
}
