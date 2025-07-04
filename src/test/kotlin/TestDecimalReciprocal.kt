package com.decimal128

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.*
import kotlin.math.abs

class TestDecimalReciprocal{

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
        val tc = TC("6")
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
        val scalePow10 = 36
        val scaleDouble = 1e36

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

        val dGuess0 = 1.0 / dDivisor * scaleDouble
        val cGuess = Coeff().apply { coeffSet(dGuess0) }

        if (verbose) {
            println("divisor:$divisor dDivisor:$dDivisor")
            println("dGuess0:$dGuess0 cGuess:$cGuess")
        }

        val coeff2xS = Coeff().apply { coeffSetPow10(scalePow10); coeffMutateShiftLeft(1) }
        val t = Coeff()

        t.coeffSetMul(divisor, cGuess)
        t.coeffSetSub(coeff2xS, t)
        t.coeffSetMul(t, cGuess)
        cGuess.coeffSetScaleDownPow10(t, scalePow10)

        if (verbose) {
            println("cGuess:$cGuess")
        }

        t.coeffSetMul(divisor, cGuess)
        t.coeffSetSub(coeff2xS, t)
        t.coeffSetMul(t, cGuess)
        cGuess.coeffSetScaleDownPow10(t, scalePow10)

        if (verbose) {
            println("cGuess:$cGuess")
        }



    }

}
