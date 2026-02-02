package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.*

class TestInvDoubleDouble{

    // FIXME this test is incomplete/broken

    val verbose = false

    class TC(val bd: BigDecimal) {
        constructor(str: String) : this(BigDecimal(str))
        val inv0 = BigDecimal.ONE.divide(bd, MathContext.DECIMAL128)
        val inv0Product = inv0.multiply(bd)
        val isPerfect = inv0Product.compareTo(BigDecimal.ONE) == 0
        val zeroPadding = 34 - inv0.precision()
        val scale = bd.scale()
        val inv = inv0
        /*
        val sqrt = if (isPerfect) {
            if ((scale and 1) != 0 && (bd.unscaledValue().mod(BigInteger.TEN).signum() == 0)) {
                sqrt0.setScale((scale + 1) / 2)
            } else {
                sqrt0
            }
        } else {
            sqrt0.setScale(sqrt0.scale() + zeroPadding, RoundingMode.UNNECESSARY)
        }
         */
    }

    val tcs = arrayOf (
        TC("1"),
        TC("2"),
        TC("10"),
        TC("4"),
        TC("16"),
        TC("256"),
        TC("400"),
        TC("40000"),
        TC("4000000"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    @Test
    fun testProblemChild() {
        val tc = TC("3")
        test1(tc)
    }

    //@Test
    fun testRandom() {
        for (i in 0..<100000) {
            val bd = randBd()
            if (verbose)
                println("bd:$bd")
            if (bd.equals(BigDecimal.ZERO))
                continue
            val tc = TC(randBd())
            test1(tc)
        }

    }

    val random = Random()

    fun randBd() : BigDecimal {
        val bitLength = random.nextInt(0, 112)
        val bi = BigInteger(bitLength, random)
        val exp = random.nextInt(3*4096) - 6176
        val bd = BigDecimal(bi).scaleByPowerOfTen(exp)
        return bd
    }

    fun test1(tc: TC) {
        if (verbose)
            println("tc.bd:${tc.bd} tc.inv:${tc.inv}")
        val dec = MutDec()
        val decInv = MutDec()
        dec.set(tc.bd)
        val env = DecContext()
        setInv(decInv, dec, env)
        val expected = tc.inv
        //assertEquals(expected.unscaledValue(), decInv.coeffToBigInteger())
        //assertEquals(-expected.scale(), decInv.qExp)

        //TODO get setInv() to work using Newton-Raphson
    }

    fun setInv(inv: MutDec, divisor: MutDec, ctx: DecContext) {
        if (divisor.bitLen <= 1) {
            if (divisor.bitLen == 0) {
                inv.setInfinite(divisor.sign)
                return
            }
            // divisor == 1
            inv.set(divisor)
            inv.qExp = -inv.qExp
            return
        }

        val dDivisor = divisor.c256ToFloorDouble()
        val ddDivisor = divisor.c256ToNewDoubleDouble()

        val dGuess0 = 1.0 / dDivisor
        val r = Math.fma(-dDivisor, dGuess0, 1.0)
        val ddGuess0 = DoubleDouble(dGuess0, r * dGuess0)

        if (verbose) {
            println("divisor:$divisor dDivisor:$dDivisor ddDivisor:$ddDivisor")
            println("dGuess0:$dGuess0 ddGuess0:$ddGuess0")
        }

        val ddTwo = DoubleDouble(2.0, 0.0)
        val t = DoubleDouble.newMulBetter(ddDivisor, ddGuess0)
        t.setSub(ddTwo, t)
        t.setMulBetter(t, ddGuess0)
        val ddGuess1 = t

        if (verbose)
            println("ddGuess1:$ddGuess1")

    }

    @Test
    fun testDdReciprocal() {
        val ddX = DoubleDouble(3.0, 0.0)
        val ddRecip = ddReciprocal(ddX)
        println("ddX:$ddX ddRecip:$ddRecip")
    }

    fun ddReciprocal(a: DoubleDouble): DoubleDouble {
        require(!(a.hi == 0.0 && a.lo == 0.0)) { "Division by zero" }

        // 0) Constants
        val two = DoubleDouble(2.0, 0.0)

        // 1) Initial guess (just a hardware divide)
        var x = DoubleDouble(1.0 / a.hi, 0.0)

        // 2) Compute a*x in double-double
        val ax = DoubleDouble.newMulBetter(a, x)

        // 3) Compute (2 – a*x)
        val t  = DoubleDouble.newSub(two, ax)

        // 4) Multiply x * t → x_{new}
        x = DoubleDouble.newMulBetter(x, t)

        return x
    }

}
