package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.*

class TestMutDecMagnitudeDiv {

    val verbose = false

    class TC(val bdAraw: BigDecimal, val bdBraw: BigDecimal, val ctx: DecContext) {
        constructor(strA: String, strB: String, rd: RoundingDirection) :
                this(BigDecimal(strA), BigDecimal(strB), DecContext.decimal128Kotlin().with(rd))
        constructor(strA: String, strB: String) :
                this(BigDecimal(strA), BigDecimal(strB), DecContext.decimal128Kotlin())
        constructor(bdA: BigDecimal, bdB: BigDecimal) : this(bdA, bdB, DecContext.decimal128Kotlin())

        val rm = ctx.roundingDirection.mapToRoundingMode()
        val bdA = bdToIeeeDecimal128(bdAraw, rm)
        val bdAIsFinite = bdIsFinite(bdA)
        val bdB = bdToIeeeDecimal128(bdBraw, rm)
        val bdBIsFinite = bdIsFinite(bdB)
        val bdDiv = bdA.divide(bdB, MathContext(70, rm))
        val bdQ = bdToIeeeDecimal128(bdDiv, rm)
        val bdQIsInfinite = bdIsInfinite(bdQ)
    }

    val cases = arrayOf(
        TC("1.05138115419E-1636", "1.297600068158632391583639985E+4509", RoundingDirection.TOWARD_POSITIVE),
        TC("5.8331E+4541", "1.06765154850769863710859340415777E-5653", RoundingDirection.TOWARD_POSITIVE),
        TC("8.62772233398E+826", "3.37229718376401328925734846797201E-3185"),
        TC("1", "2"),
        TC("0E+4519", "4.14999526830484824E+2722"),
        TC("2.7710284E-1295", "2.912E-5964"),
        TC("3.4648355837009412658250388928553E-289", "1.432458417443E-546"),
        TC("2.76087719145005779930318E+4433", "8.24163109571752684E+5964", RoundingDirection.TOWARD_POSITIVE),
        TC("1.221824056626775696489E-5049", "6.4667951153346922410790519767E-4095", RoundingDirection.TIES_TO_AWAY),
        TC("3.936175555033646832418361E+4916", "1.9547932317865978101179491106E+3786",RoundingDirection.TIES_TO_EVEN),
        TC("3.936175555033646832418361E+4916", "1.9547932317865978101179491106E+3786",RoundingDirection.TOWARD_ZERO),
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

    fun randDecimal128Context(): DecContext {
        val i = random.nextInt(4)
        val env = DecContext.decimal128Kotlin().with(RoundingDirection.fromValue(i))
        return env
    }

    fun test1(tc: TC) {
        val bdA = tc.bdA
        val bdB = tc.bdB
        val expected = tc.bdQ
        val env = tc.ctx
        val rm = env.roundingDirection.mapToRoundingMode()

        if (verbose)
            println("bdA:$bdA / bdB:$bdB (rm:$rm) => expected:$expected")

        val decA = newMutDec(bdA)
        val decB = newMutDec(bdB)
        val decQ = MutDec()
        decQ.setDiv(decA, decB, env)
        if (verbose)
            println("magQ:$decQ")
        val biExpected = expected.unscaledValue()
        val qExpExpected = -expected.scale()
        val expectInfinite = tc.bdQIsInfinite
        val biObserved = decQ.coeffToBigInteger()
        val qExpObserved = decQ.qExp
        val observedInfinite = decQ.isInfinite()
        if (verbose || expectInfinite != observedInfinite ||
            !expectInfinite && qExpExpected != qExpObserved) {
            println("bdA:$bdA / bdB:$bdB (rm:$rm) => expected:$expected")
            println(" => expectInfinite:$expectInfinite observedInfinite:$observedInfinite")
            println(" => qExpExpected:$qExpExpected qExpObserved:$qExpObserved")
            println(" => biExpected:$biExpected biObserved:$biObserved")
        }
        assertEquals(expectInfinite, observedInfinite)
        if (expectInfinite)
            return
        assertEquals(biExpected, biObserved)
        assertEquals(qExpExpected, qExpObserved)
    }
}
