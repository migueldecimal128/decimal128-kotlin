package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.*

class TestDecimalMagnitudeDiv {

    val verbose = false

    class TC(val bdAraw: BigDecimal, val bdBraw: BigDecimal, val ctx: DecimalContext) {
        constructor(strA: String, strB: String, rd: RoundingDirection) :
                this(BigDecimal(strA), BigDecimal(strB), DecimalContext(rd))
        constructor(strA: String, strB: String) :
                this(BigDecimal(strA), BigDecimal(strB), DecimalContext())
        constructor(bdA: BigDecimal, bdB: BigDecimal) : this(bdA, bdB, DecimalContext())

        val rm = ctx.roundingDirection.mapToRoundingMode()
        val bdA = bdToIeeeDecimal128(bdAraw, rm)
        val bdAIsFinite = bdIsFinite(bdA)
        val bdB = bdToIeeeDecimal128(bdBraw, rm)
        val bdBIsFinite = bdIsFinite(bdB)
        val bdDiv = bdA.divide(bdB, MathContext(70, rm))
        val bdP = bdToIeeeDecimal128(bdDiv, rm)
    }

    val cases = arrayOf(
        TC("8.62772233398E+826", "3.37229718376401328925734846797201E-3185"),
        TC("1", "2"),
        TC("0E+4519", "4.14999526830484824E+2722"),
        TC("2.7710284E-1295", "2.912E-5964"),
        TC("3.4648355837009412658250388928553E-289", "1.432458417443E-546"),
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
        //val tc = TC("8.62772233398E+826", "3.37229718376401328925734846797201E-3185")
        val tc = TC("862772233398", "337229718376401328925734846797201")
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

    fun randDecimal128Context(): DecimalContext {
        val i = random.nextInt(4)
        val ctx = DecimalContext(RoundingDirection.fromValue(i))
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

        val decA = newDecimal(bdA)
        val decB = newDecimal(bdB)
        val decQ = Decimal()
        decQ.setDivide(decA, decB, ctx)
        if (verbose)
            println("magQ:$decQ")
        val biExpected = expected.unscaledValue()
        val qExpExpected = -expected.scale()
        val biObserved = decQ.coeffToBigInteger()
        val qExpObserved = decQ.qExp
        if (verbose || biExpected != biObserved || qExpExpected != qExpObserved) {
            println("bdA:$bdA / bdB:$bdB (rm:$rm) => expected:$expected")
            println(" => qExpExpected:$qExpExpected qExpObserved:$qExpObserved")
            println(" => biExpected:$biExpected biObserved:$biObserved")
        }
        assertEquals(biExpected, biObserved)
        assertEquals(qExpExpected, qExpObserved)
    }
}
