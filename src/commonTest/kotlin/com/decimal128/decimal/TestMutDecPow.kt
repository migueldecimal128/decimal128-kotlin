package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertTrue

class TestMutDecPow {


    private fun md(s: String) = MutDec().set(s)
    private fun pow(x: String, n: Int): MutDec = MutDec().setPow(md(x), n, DecContext.current())

    // ---- pow == 0 ------------------------------------------------------------

    @Test
    fun finiteToZero() = assertTrue(pow("3.7", 0) bitwiseEQ md("1E+0"))
    @Test
    fun negFiniteToZero() = assertTrue(pow("-3.7", 0) bitwiseEQ md("1E+0"))
    @Test
    fun posZeroToZero() = assertTrue(pow("0", 0) bitwiseEQ md("1E+0"))
    @Test
    fun negZeroToZero() = assertTrue(pow("-0", 0) bitwiseEQ md("1E+0"))
    @Test
    fun posInfToZero() = assertTrue(pow("Inf", 0) bitwiseEQ md("1E+0"))
    @Test
    fun negInfToZero() = assertTrue(pow("-Inf", 0) bitwiseEQ md("1E+0"))

    // ---- pow == 1 ------------------------------------------------------------

    @Test
    fun finiteToOne() = assertTrue(pow("3.7", 1) bitwiseEQ md("3.7"))
    @Test
    fun negFiniteToOne() = assertTrue(pow("-3.7", 1) bitwiseEQ md("-3.7"))

    // ---- pow == 2 ------------------------------------------------------------

    @Test
    fun squarePositive() = assertTrue(pow("3", 2) bitwiseEQ md("9"))
    @Test
    fun squareNegative() = assertTrue(pow("-3", 2) bitwiseEQ md("9"))

    // ---- normal finite values ------------------------------------------------

    @Test
    fun posIntCubePow() = assertTrue(pow("2", 3) bitwiseEQ md("8"))
    @Test
    fun posIntLargePow() = assertTrue(pow("10", 10) bitwiseEQ md("10_000_000_000"))
    @Test
    fun posIntLargePow2() = assertTrue(pow("10", 10) EQ md("1e10"))
    @Test
    fun negBaseEvenPow() = assertTrue(pow("-3", 4) bitwiseEQ md("81"))
    @Test
    fun negBaseOddPow() = assertTrue(pow("-3", 3) bitwiseEQ md("-27"))
    @Test
    fun negBaseLargeEvenPow() = assertTrue(pow("-3", 4) bitwiseEQ md("81"))
    @Test
    fun negBaseLargeOddPow() = assertTrue(pow("-3", 5) bitwiseEQ md("-243"))

    // ---- negative pow --------------------------------------------------------

    @Test
    fun reciprocal() = assertTrue(pow("2", -1) bitwiseEQ md("0.5"))
    @Test
    fun negPowEven() = assertTrue(pow("-2", -2) bitwiseEQ md("0.25"))
    @Test
    fun negPowOdd() = assertTrue(pow("-2", -1) bitwiseEQ md("-0.5"))

    // ---- zero base -----------------------------------------------------------

    @Test
    fun posZeroPosPow() = assertTrue(pow("0", 3) bitwiseEQ md("0"))
    @Test
    fun negZeroOddPow() = assertTrue(pow("-0", 3) bitwiseEQ md("-0"))
    @Test
    fun negZeroEvenPow() = assertTrue(pow("-0", 4) bitwiseEQ md("0"))

    @Test
    fun posZeroNegPow() {
        val ctx = DecContext.decimal128IEEE()
        val result = MutDec().setPow(md("0"), -1, ctx)
        assertTrue(result bitwiseEQ md("INF"))
        assertTrue(ctx.isSet(DecException.DIVIDE_BY_ZERO))
    }

    @Test
    fun negZeroNegOddPow() {
        val ctx = DecContext.decimal128IEEE()
        val result = MutDec().setPow(md("-0"), -1, ctx)
        assertTrue(result bitwiseEQ md("-infinity"))
        assertTrue(ctx.isSet(DecException.DIVIDE_BY_ZERO))
    }

    @Test
    fun negZeroNegEvenPow() {
        val ctx = DecContext.decimal128IEEE()
        val result = MutDec().setPow(md("-0"), -2, ctx)
        assertTrue(result.isInfinite() && !result.sign)
        assertTrue(ctx.isSet(DecException.DIVIDE_BY_ZERO))
    }

    // ---- zero base exponent cohort -------------------------------------------

    @Test
    fun zeroExpCohort() {
        // 0E+2 ^ 3 = 0E+6
        assertTrue(pow("0E+2", 3) bitwiseEQ md("0E+6"))
    }

    @Test
    fun negZeroExpCohortOdd() {
        // -0E+1 ^ 3 = -0E+3
        assertTrue(pow("-0E+1", 3) bitwiseEQ md("-0E+3"))
    }

    @Test
    fun negZeroExpCohortEven() {
        // -0E+1 ^ 4 = 0E+4
        assertTrue(pow("-0E+1", 4) bitwiseEQ md("0E+4"))
    }

    // ---- infinity base -------------------------------------------------------

    @Test
    fun posInfPosPow() = assertTrue(pow("Inf", 3) bitwiseEQ md("Inf"))
    @Test
    fun negInfOddPow() = assertTrue(pow("-Inf", 3) bitwiseEQ md("-Inf"))
    @Test
    fun negInfEvenPow() = assertTrue(pow("-Inf", 4) bitwiseEQ md("Inf"))
    @Test
    fun posInfNegPow() = assertTrue(pow("Inf", -1) bitwiseEQ md("0"))
    @Test
    fun negInfNegOddPow() = assertTrue(pow("-Inf", -1) bitwiseEQ md("-0"))
    @Test
    fun negInfNegEvenPow() = assertTrue(pow("-Inf", -2) bitwiseEQ md("0"))

    // ---- NaN -----------------------------------------------------------------

    @Test
    fun qNaNAnyPow() {
        val result = pow("NaN", 5)
        assertTrue(result.isNaN() && !result.isSignaling())
    }

    @Test
    fun qNaNZeroPow() {
        // NaN^0 = NaN, not 1
        val result = pow("NaN", 0)
        assertTrue(result.isNaN() && !result.isSignaling())
    }

    @Test
    fun sNaNAnyPow() {
        val ctx = DecContext.decimal128IEEE()
        val result = MutDec().setPow(md("sNaN"), 5, ctx)
        assertTrue(ctx.isSet(DecException.INVALID_OPERATION))
        assertTrue(result.isNaN() && !result.isSignaling())
    }

    @Test
    fun sNaNZeroPow() {
        // sNaN^0 = qNaN + Invalid, not 1
        val ctx = DecContext.decimal128IEEE()
        val result = MutDec().setPow(md("sNaN"), 0, ctx)
        assertTrue(ctx.isSet(DecException.INVALID_OPERATION))
        assertTrue(result.isNaN() && !result.isSignaling())
    }

    // ---- large pow / overflow / underflow ------------------------------------

    @Test
    fun overflowPosToInfinity() {
        val ctx = DecContext.decimal128IEEE()
        val result = MutDec().setPow(md("1E+100"), 1000, ctx)
        assertTrue(result.isInfinite() && !result.sign)
        assertTrue(ctx.isSet(DecException.OVERFLOW))
    }

    @Test
    fun overflowNegOddToNegInfinity() {
        val ctx = DecContext.decimal128IEEE()
        val result = MutDec().setPow(md("-1E+100"), 1001, ctx)
        assertTrue(result.isInfinite() && result.sign)
        assertTrue(ctx.isSet(DecException.OVERFLOW))
    }

    @Test
    fun overflowNegEvenToPosInfinity() {
        val ctx = DecContext.decimal128IEEE()
        val result = MutDec().setPow(md("-1E+100"), 1000, ctx)
        assertTrue(result.isInfinite() && !result.isNegative())
        assertTrue(ctx.isSet(DecException.OVERFLOW))
    }

    @Test
    fun underflowToZero() {
        val ctx = DecContext.decimal128IEEE()
        val result = MutDec().setPow(md("1E-100"), 1000, ctx)
        assertTrue(result bitwiseEQ MutDec().setZeroWithQTiny(sign = false))
        assertTrue(ctx.isSet(DecException.UNDERFLOW))
    }

    @Test
    fun underflowNegOddToNegZero() {
        val ctx = DecContext.decimal128IEEE()
        val result = MutDec().setPow(md("-1E-100"), 1001, ctx)
        assertTrue(result bitwiseEQ md("-0E-6176"))
        assertTrue(ctx.isSet(DecException.UNDERFLOW))
    }
}