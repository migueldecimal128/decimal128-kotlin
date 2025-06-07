package com.decimal128

import com.decimal128.TestMagMul.TC
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.util.*

class TestMagDiv {

    val verbose = true

    class TC(val bdAraw: BigDecimal, val bdBraw: BigDecimal, val ctx: Decimal128Context) {
        constructor(strA: String, strB: String, rd: RoundingDirection) :
                this(BigDecimal(strA), BigDecimal(strB), Decimal128Context(rd))
        constructor(strA: String, strB: String) :
                this(BigDecimal(strA), BigDecimal(strB), Decimal128Context())
        constructor(bdA: BigDecimal, bdB: BigDecimal) : this(bdA, bdB, Decimal128Context())

        val rm = ctx.roundingDirection.mapToRoundingMode()
        val bdA = bdToIeeeDecimal128(bdAraw, rm)
        val bdAIsFinite = bdIsFinite(bdA)
        val bdB = bdToIeeeDecimal128(bdBraw, rm)
        val bdBIsFinite = bdIsFinite(bdB)
        val bdDiv = bdA.divide(bdB, MathContext(36, rm))
        val bdP = bdToIeeeDecimal128(bdDiv, rm)
    }

    val cases = arrayOf(
        TC("2.76087719145005779930318E+4433", "8.24163109571752684E+5964", RoundingDirection.ROUND_TOWARD_POSITIVE),
        TC("1.221824056626775696489E-5049", "6.4667951153346922410790519767E-4095", RoundingDirection.ROUND_TIES_TO_AWAY),
        TC("3.936175555033646832418361E+4916", "1.9547932317865978101179491106E+3786",RoundingDirection.ROUND_TIES_TO_EVEN),
        TC("3.936175555033646832418361E+4916", "1.9547932317865978101179491106E+3786",RoundingDirection.ROUND_TOWARD_ZERO),
        TC("1", "3"),
        TC("1", "2"),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testProblemChild() {
        //FIXME - the problem is this
        // I have stripped off trailing zeros before rounding
        // ROUND_TOWARD_POSITIVE == CEILING
        // I stripped off a zero that should have become a 1
        // If the last zero had remained behind, then I would have been OK
        // I think this says that if roundingDirection.roundUp()
        // then stop stripping zeros at 34 digits?
        // Expected :3349916004957623421475058406806671
        // Actual   :334991600495762342147505840680668
        val tc = TC("2.76087719145005779930318E+4433", "8.24163109571752684E+5964", RoundingDirection.ROUND_TOWARD_POSITIVE)
        test1(tc)
    }



    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val bdA = randBd()
            var bdB: BigDecimal
            do {
                bdB = randBd()
            } while (bdB.signum() == 0)
            val tc = TC(bdA, bdB, randDecimal128Context())
            if (tc.bdAIsFinite && tc.bdBIsFinite)
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

    fun randDecimal128Context(): Decimal128Context {
        val i = random.nextInt(4)
        val ctx = Decimal128Context(RoundingDirection.fromValue(i))
        return ctx
    }

    fun test1(tc: TC) {
        val bdA = tc.bdA
        val bdB = tc.bdB
        val expected = tc.bdP
        val ctx = tc.ctx
        val rm = ctx.roundingDirection.mapToRoundingMode()

        if (verbose)
            println("bdA:$bdA / bdB:$bdB (rm:$rm) => expected:$expected")

        val magA = Mag(bdA)
        val magB = Mag(bdB)
        val magQ = Mag()
        magQ.magDiv(magA, magB, 0, ctx)
        if (verbose)
            println("magQ:$magQ")
        assertEquals(expected.unscaledValue(), magQ.coeffToBigInteger())
        assertEquals(-expected.scale(), magQ.qExp)
    }

}
