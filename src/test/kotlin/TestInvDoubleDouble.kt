package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.lang.Long.numberOfTrailingZeros
import java.lang.Math.unsignedMultiplyHigh
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.util.*
import kotlin.math.max
import kotlin.math.min

class TestInvDoubleDouble{

    // FIXME this test is incomplete/broken

    val verbose = true

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
        val dec = Decimal()
        val decInv = Decimal()
        dec.set(tc.bd)
        val ctx = DecimalContext.newDecimal128Context()
        setInv(decInv, dec, ctx)
        val expected = tc.inv
        //assertEquals(expected.unscaledValue(), decInv.coeffToBigInteger())
        //assertEquals(-expected.scale(), decInv.qExp)

        //TODO get setInv() to work using Newton-Raphson
    }

    fun setInv(inv: Decimal, divisor: Decimal, ctx: DecimalContext) {
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
        val dDivisor = divisor.coeffToFloorDouble()
        val dGuess0 = 1.0 / dDivisor
        val ddDivisor = divisor.coeffToNewDoubleDouble()

        val ddTwo = DoubleDouble(2.0, 0.0)
        val tmp = DoubleDouble.newMulDoubleDoubleByDouble(ddDivisor, dGuess0)
        tmp.setSub(ddTwo, tmp)
        tmp.setMulDoubleDoubleByDouble(tmp, dGuess0)
        val ddGuess1 = tmp

        if (verbose) {
            println("divisor:$divisor dDivisor:$dDivisor ddDivisor:$ddDivisor")
            println("dGuess0:$dGuess0, ddGuess1:$ddGuess1")
        }

        val p = ctx.precision
        val guard = 3
        val d = divisor.digitLen
        val s = p + guard + (d - 1)
        val z0 = Coeff().apply { coeffSetPow10(s) }



    }

}
