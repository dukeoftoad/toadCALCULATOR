// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.math

/**
 * Public entry point of the math engine.
 *
 * ```
 * evaluate("0.1+0.2", AngleMode.DEG)  // Success(0.3)
 * evaluate("1/0")                     // Failure(DivisionByZero)
 * evaluate("tan(90)")                 // Failure(DomainError) in DEG
 * ```
 *
 * This function never throws: every internal failure is mapped to
 * [MathResult.Failure]. The default angle mode is DEG (matches the app default).
 */
fun evaluate(input: String, angleMode: AngleMode = AngleMode.DEG): MathResult {
    return try {
        val tokens = Lexer(input).lex()
        val ast = Parser(tokens, inputLength = input.length).parse()
        val value = Evaluator(angleMode).eval(ast)
        MathResult.Success(value)
    } catch (e: MathSyntaxException) {
        MathResult.Failure(e.error)
    } catch (e: MathEvalException) {
        MathResult.Failure(e.error)
    } catch (e: ArithmeticException) {
        // Defensive: any BigDecimal misbehavior maps to a typed error, never a crash.
        MathResult.Failure(MathError.Overflow)
    } catch (e: Exception) {
        MathResult.Failure(MathError.Undefined)
    } catch (e: OutOfMemoryError) {
        throw e // genuine VM exhaustion must not be swallowed
    } catch (e: Throwable) {
        MathResult.Failure(MathError.Undefined)
    }
}
