package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertTrue

class TestDecimalPow {


    private fun dec(s: String): Decimal = Decimal.from(s)
    private fun pow(x: String, n: Int): Decimal = x.toDecimal().pow(n)

    // ---- pow == 0 ------------------------------------------------------------

    @Test
    fun finiteToZero() = assertTrue(pow("3.7", 0) bitwiseEQ dec("1E+0"))
    @Test
    fun negFiniteToZero() = assertTrue(pow("-3.7", 0) bitwiseEQ dec("1E+0"))
    @Test
    fun posZeroToZero() = assertTrue(pow("0", 0) bitwiseEQ dec("1E+0"))
    @Test
    fun negZeroToZero() = assertTrue(pow("-0", 0) bitwiseEQ dec("1E+0"))
    @Test
    fun posInfToZero() = assertTrue(pow("Inf", 0) bitwiseEQ dec("1E+0"))
    @Test
    fun negInfToZero() = assertTrue(pow("-Inf", 0) bitwiseEQ dec("1E+0"))

    // ---- pow == 1 ------------------------------------------------------------

    @Test
    fun finiteToOne() = assertTrue(pow("3.7", 1) bitwiseEQ dec("3.7"))
    @Test
    fun negFiniteToOne() = assertTrue(pow("-3.7", 1) bitwiseEQ dec("-3.7"))

    // ---- pow == 2 ------------------------------------------------------------

    @Test
    fun squarePositive() = assertTrue(pow("3", 2) bitwiseEQ dec("9"))
    @Test
    fun squareNegative() = assertTrue(pow("-3", 2) bitwiseEQ dec("9"))

    // ---- normal finite values ------------------------------------------------

    @Test
    fun posIntCubePow() = assertTrue(pow("2", 3) bitwiseEQ dec("8"))
    @Test
    fun posIntLargePow() = assertTrue(pow("10", 10) bitwiseEQ dec("10_000_000_000"))
    @Test
    fun posIntLargePow2() = assertTrue(pow("10", 10) EQ dec("1e10"))
    @Test
    fun negBaseEvenPow() = assertTrue(pow("-3", 4) bitwiseEQ dec("81"))
    @Test
    fun negBaseOddPow() = assertTrue(pow("-3", 3) bitwiseEQ dec("-27"))
    @Test
    fun negBaseLargeEvenPow() = assertTrue(pow("-3", 4) bitwiseEQ dec("81"))
    @Test
    fun negBaseLargeOddPow() = assertTrue(pow("-3", 5) bitwiseEQ dec("-243"))

    // ---- negative pow --------------------------------------------------------

    @Test
    fun reciprocal() = assertTrue(pow("2", -1) bitwiseEQ dec("0.5"))
    @Test
    fun negPowEven() = assertTrue(pow("-2", -2) bitwiseEQ dec("0.25"))
    @Test
    fun negPowOdd() = assertTrue(pow("-2", -1) bitwiseEQ dec("-0.5"))

    // ---- zero base -----------------------------------------------------------

    @Test
    fun posZeroPosPow() = assertTrue(pow("0", 3) bitwiseEQ dec("0"))
    @Test
    fun negZeroOddPow() = assertTrue(pow("-0", 3) bitwiseEQ dec("-0"))
    @Test
    fun negZeroEvenPow() = assertTrue(pow("-0", 4) bitwiseEQ dec("0"))

    @Test
    fun posZeroNegPow() {
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val result = Decimal.ZERO.pow(-1)
            assertTrue(result bitwiseEQ dec("INF"))
        }
        assertTrue(ctx.isSet(DecException.DIVIDE_BY_ZERO))
    }

    @Test
    fun negZeroNegOddPow() {
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val result = Decimal.NEG_ZEROe0.pow(-1)
            assertTrue(result bitwiseEQ dec("-infinity"))
        }
        assertTrue(ctx.isSet(DecException.DIVIDE_BY_ZERO))
    }

    @Test
    fun negZeroNegEvenPow() {
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val result = "-0".toDecimal().pow(-2)
            assertTrue(result.isInfinite() && !result.isNegative())
        }
        assertTrue(ctx.isSet(DecException.DIVIDE_BY_ZERO))
    }

    // ---- zero base exponent cohort -------------------------------------------

    @Test
    fun zeroExpCohort() {
        // 0E+2 ^ 3 = 0E+6
        assertTrue(pow("0E+2", 3) bitwiseEQ dec("0E+6"))
    }

    @Test
    fun negZeroExpCohortOdd() {
        // -0E+1 ^ 3 = -0E+3
        assertTrue(pow("-0E+1", 3) bitwiseEQ dec("-0E+3"))
    }

    @Test
    fun negZeroExpCohortEven() {
        // -0E+1 ^ 4 = 0E+4
        assertTrue(pow("-0E+1", 4) bitwiseEQ dec("0E+4"))
    }

    // ---- infinity base -------------------------------------------------------

    @Test
    fun posInfPosPow() = assertTrue(pow("Inf", 3) bitwiseEQ dec("Inf"))
    @Test
    fun negInfOddPow() = assertTrue(pow("-Inf", 3) bitwiseEQ dec("-Inf"))
    @Test
    fun negInfEvenPow() = assertTrue(pow("-Inf", 4) bitwiseEQ dec("Inf"))
    @Test
    fun posInfNegPow() = assertTrue(pow("Inf", -1) bitwiseEQ dec("0"))
    @Test
    fun negInfNegOddPow() = assertTrue(pow("-Inf", -1) bitwiseEQ dec("-0"))
    @Test
    fun negInfNegEvenPow() = assertTrue(pow("-Inf", -2) bitwiseEQ dec("0"))

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
        ctx.eval {
            val result = "sNaN".toDecimal().pow(5)
            assertTrue(ctx.isSet(DecException.INVALID_OPERATION))
            assertTrue(result.isNaN() && !result.isSignaling())
        }
    }

    @Test
    fun sNaNZeroPow() {
        // sNaN^0 = qNaN + Invalid, not 1
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val result = "sNaN".toDecimal().pow(0)
            assertTrue(ctx.isSet(DecException.INVALID_OPERATION))
            assertTrue(result.isNaN() && !result.isSignaling())
        }
    }

    // ---- large pow / overflow / underflow ------------------------------------

    @Test
    fun overflowPosToInfinity() {
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val result = "1E+100".toDecimal().pow(1000)
            assertTrue(result.isInfinite() && !result.isNegative())
            assertTrue(ctx.isSet(DecException.OVERFLOW))
        }
    }

    @Test
    fun overflowNegOddToNegInfinity() {
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val result = dec("-1E+100").pow(1001)
            assertTrue(result.isInfinite() && result.isNegative())
            assertTrue(ctx.isSet(DecException.OVERFLOW))
        }
    }

    @Test
    fun overflowNegEvenToPosInfinity() {
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val result = dec("-1E+100").pow(1000)
            assertTrue(result.isInfinite() && !result.isNegative())
            assertTrue(ctx.isSet(DecException.OVERFLOW))
        }
    }

    @Test
    fun underflowToZero() {
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val result = dec("1E-100").pow(1000)
            assertTrue(result bitwiseEQ dec("0E-6176"))
            assertTrue(ctx.isSet(DecException.UNDERFLOW))
        }
    }

    @Test
    fun underflowNegOddToNegZero() {
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val result = dec("-1E-100").pow(1001)
            assertTrue(result bitwiseEQ dec("-0E-6176"))
            assertTrue(ctx.isSet(DecException.UNDERFLOW))
        }
    }
}