package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.Long.compareUnsigned
import java.lang.Math.unsignedMultiplyHigh
import java.math.BigInteger
import java.math.BigInteger.*
import java.util.*

class TestSimpleRecipMulDivSmallPow10 {

    val verbose = false

    val simpleRecipPow10s = LongArray(5)
    val pow10s = LongArray(5)

    fun initializeSimpleRecipPow10s() {
        val twoPow64 = ONE.shiftLeft(64)
        pow10s[0] = 1
        for (i in 1..4) {
            val pow10 = pow10s[i-1] * 10L
            pow10s[i] = pow10
            // we want floor here ... don't round up
            val biM = twoPow64.divide(BigInteger(pow10.toString()))
            val m = biM.toLong()
            simpleRecipPow10s[i] = m
            if (verbose)
                println("simpleRecipPow10s[$i] = $m 0x${biM.toString(16)}")
        }
    }

    fun divModPow10Simple(bi: BigInteger, pow10: Int) : Pair<BigInteger, Int> {
        require(pow10 in 1..4)
        require(bi.bitLength() <= 256)

        val dw0 = bi.toLong()
        val dw1 = bi.shiftRight(64).toLong()
        val dw2 = bi.shiftRight(128).toLong()
        val dw3 = bi.shiftRight(192).toLong()

        val mask48= (1L shl 48) - 1
        val dwA = dw0 and mask48
        val dwB = ((dw1 shl -48) or (dw0 ushr 48)) and mask48
        val dwC = ((dw2 shl -32) or (dw1 ushr 32)) and mask48
        val dwD = ((dw3 shl -16) or (dw2 ushr 16)) and mask48
        val dwE = dw3

        assertEquals(bi.toLong() and mask48, dwA)
        assertEquals(bi.shiftRight(48).toLong() and mask48, dwB)
        assertEquals(bi.shiftRight(96).toLong() and mask48, dwC)
        assertEquals(bi.shiftRight(144).toLong() and mask48, dwD)
        assertEquals(bi.shiftRight(192).toLong(), dwE)

        val denom = pow10s[pow10]
        val M = simpleRecipPow10s[pow10]

        val qEhat = unsignedMultiplyHigh(dwE, M)
        val rEhat = dwE - (qEhat * denom)
        val adjustE = if (rEhat >= denom) denom else 0L
        val qE = qEhat + adjustE
        val rE = rEhat - adjustE

        val qDhat = unsignedMultiplyHigh((rE shl 48) or dwD, M)
        val rDhat = dwD - (qDhat * denom)
        val adjustD = if (rDhat >= denom) denom else 0L
        val qD = qDhat + adjustD
        val rD = rDhat - adjustD

        val qChat = unsignedMultiplyHigh((rD shl 48) or dwC, M)
        val rChat = dwC - (qChat * denom)
        val adjustC = if (rChat >= denom) denom else 0L
        val qC = qChat + adjustC
        val rC = rChat - adjustC

        val qBhat = unsignedMultiplyHigh((rC shl 48) or dwB, M)
        val rBhat = dwB - (qBhat * denom)
        val adjustB = if (rBhat >= denom) denom else 0L
        val qB = qBhat + adjustB
        val rB = rBhat - adjustB

        val qAhat = unsignedMultiplyHigh((rB shl 48) or dwA, M)
        val rAhat = dwA - (qAhat * denom)
        val adjustA = if (rAhat >= denom) denom else 0L
        val qA = qAhat + adjustA
        val rA = rAhat - adjustA

        val biQ =
            BigInteger(""+qE).shiftLeft(192).or(BigInteger(""+qD).shiftLeft(144))
                .or(BigInteger(""+qC).shiftLeft(96)).or(BigInteger(""+qB).shiftLeft(48)).or(BigInteger(""+qA))
        return biQ to rA.toInt()
    }

    @Test
    fun testSimple() {
        initializeSimpleRecipPow10s()

        val bi1 = BigInteger(0x111111111111L.toString())
        val bi2 = BigInteger(0x222222222222L.toString())
        val bi3 = BigInteger(0x333333333333L.toString())
        val bi4 = BigInteger(0x444444444444L.toString())
        val bi5 = BigInteger(0x5555555555555555L.toString())
        val bi54321 = bi5.shiftLeft(192).or(bi4.shiftLeft(144)).or(bi3.shiftLeft(96)).or(bi2.shiftLeft(48)).or(bi1)
        val pow10 = 1
        val (expectedQ, expectedR) = bi54321.divideAndRemainder(TEN.pow(pow10))
        if (verbose)
            println("bi:$bi54321 0x${bi54321.toString(16)} pow10:$pow10 expectedQ:$expectedQ expectedR:$expectedR")

        val (observedQ, observedR) = divModPow10Simple(bi54321, pow10)

        assertEquals(expectedQ, observedQ)
        assertEquals(expectedR.toInt(), observedR)
    }

    val simpleRecipPow10sDiv2 = LongArray(6)
    val pow10sDiv2 = LongArray(6)

    fun initializeSimpleRecipPow10sDiv2() {
        val twoPow64 = ONE.shiftLeft(64)
        for (i in 1..5) {
            val pow10Div2 = if (i == 1) 5L else pow10sDiv2[i-1] * 10L
            pow10sDiv2[i] = pow10Div2
            val biM = twoPow64.divide(BigInteger(pow10Div2.toString()))
            val m = biM.toLong()
            simpleRecipPow10sDiv2[i] = m
            if (verbose)
                println("simpleRecipPow10sDiv2[$i] = $m 0x${biM.toString(16)}")
        }
    }

    fun simpleRecipDivModPow10Div2(bi: BigInteger, pow10: Int) : Pair<BigInteger, Int> {
        require(pow10 in 1..5)
        require(bi.bitLength() <= 256)

        val biDiv2 = bi.shiftRight(1)

        val mask48= (1L shl 48) - 1
        val biDiv2A = biDiv2.toLong() and mask48
        val biDiv2B = biDiv2.shiftRight(48).toLong() and mask48
        val biDiv2C = biDiv2.shiftRight(96).toLong() and mask48
        val biDiv2D = biDiv2.shiftRight(144).toLong() and mask48
        val biDiv2E = biDiv2.shiftRight(192).toLong()

        val dw0 = bi.toLong()
        val dw1 = bi.shiftRight(64).toLong()
        val dw2 = bi.shiftRight(128).toLong()
        val dw3 = bi.shiftRight(192).toLong()

        val dwA = (dw0 ushr 1) and mask48
        val dwB = ((dw1 shl -49) or (dw0 ushr 49)) and mask48
        val dwC = ((dw2 shl -33) or (dw1 ushr 33)) and mask48
        val dwD = ((dw3 shl -17) or (dw2 ushr 17)) and mask48
        val dwE = (dw3 ushr 1)

        //assertEquals(biDiv2A, dwA)
        //assertEquals(biDiv2B, dwB)
        //assertEquals(biDiv2C, dwC)
        //assertEquals(biDiv2D, dwD)
        //assertEquals(biDiv2E, dwE)

        val denom = pow10sDiv2[pow10]
        val M = simpleRecipPow10sDiv2[pow10]

        val qEhat = unsignedMultiplyHigh(dwE, M)
        val rEhat = dwE - (qEhat * denom)
        val adjustE = rEhat >= denom
        val qE = qEhat + if (adjustE) 1L else 0L
        val rE = rEhat - if (adjustE) denom else 0L

        val ppD = (rE shl 48) or dwD
        val qDhat = unsignedMultiplyHigh(ppD, M)
        val rDhat = ppD - (qDhat * denom)
        val adjustD = rDhat >= denom
        val qD = qDhat + if (adjustD) 1L else 0L
        val rD = rDhat - if (adjustD) denom else 0L

        val ppC = (rD shl 48) or dwC
        val qChat = unsignedMultiplyHigh(ppC, M)
        val rChat = ppC - (qChat * denom)
        val adjustC = rChat >= denom
        val qC = qChat + if (adjustC) 1L else 0L
        val rC = rChat - if (adjustC) denom else 0L

        val ppB = (rC shl 48) or dwB
        val qBhat = unsignedMultiplyHigh(ppB, M)
        val rBhat = ppB - (qBhat * denom)
        val adjustB = rBhat >= denom
        val qB = qBhat + if (adjustB) 1L else 0L
        val rB = rBhat - if (adjustB) denom else 0L

        val ppA = (rB shl 48) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, M)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = ((rA shl 1) or (dw0 and 1)).toInt()

        val biQ =
            BigInteger(""+qE).shiftLeft(192).or(BigInteger(""+qD).shiftLeft(144))
                .or(BigInteger(""+qC).shiftLeft(96)).or(BigInteger(""+qB).shiftLeft(48)).or(BigInteger(""+qA))

        if (verbose)
            println("bi:$bi / 10**$pow10 => $biQ ($remainder)")


        return biQ to remainder
    }

    @Test
    fun testSimpleDivModPow10Div2() {
        initializeSimpleRecipPow10sDiv2()

        val bi1 = BigInteger(0x111111111111L.toString())
        val bi2 = BigInteger(0x222222222222L.toString())
        val bi3 = BigInteger(0x333333333333L.toString())
        val bi4 = BigInteger(0x444444444444L.toString())
        val bi5 = BigInteger(0x5555555555555555L.toString())
        val bi54321 = bi5.shiftLeft(192).or(bi4.shiftLeft(144)).or(bi3.shiftLeft(96)).or(bi2.shiftLeft(48)).or(bi1)
        val pow10 = 1
        val (expectedQ, expectedR) = bi54321.divideAndRemainder(TEN.pow(pow10))
        if (verbose)
            println("bi:$bi54321 0x${bi54321.toString(16)} pow10:$pow10 expectedQ:$expectedQ expectedR:$expectedR")

        val (observedQ, observedR) = simpleRecipDivModPow10Div2(bi54321, pow10)

        assertEquals(expectedQ, observedQ)
        assertEquals(expectedR.toInt(), observedR)
    }

    @Test
    fun test1024() {
        initializeSimpleRecipPow10sDiv2()

        val bi1024 = BigInteger("1024")
        val pow10 = 1
        val (expectedQ, expectedR) = bi1024.divideAndRemainder(TEN.pow(pow10))
        if (verbose)
            println("bi:$bi1024 0x${bi1024.toString(16)} pow10:$pow10 expectedQ:$expectedQ expectedR:$expectedR")

        val (observedQ, observedR) = simpleRecipDivModPow10Div2(bi1024, pow10)

        assertEquals(expectedQ, observedQ)
        assertEquals(expectedR.toInt(), observedR)
    }

    @Test
    fun testMaxInt256() {
        initializeSimpleRecipPow10sDiv2()

        val bi256 = ONE.shiftLeft(256).subtract(ONE)
        val pow10 = 1
        val (expectedQ, expectedR) = bi256.divideAndRemainder(TEN.pow(pow10))
        if (verbose)
            println("bi:$bi256 0x${bi256.toString(16)} pow10:$pow10 expectedQ:$expectedQ expectedR:$expectedR")

        val (observedQ, observedR) = simpleRecipDivModPow10Div2(bi256, pow10)

        assertEquals(expectedQ, observedQ)
        assertEquals(expectedR.toInt(), observedR)
    }

    inner class TC(val bi: BigInteger, val pow10: Int) {
        constructor(biStr: String, pow10: Int) : this(BigInteger(biStr), pow10)

        val qr = bi.divideAndRemainder(TEN.pow(pow10))
        val expectedQ = qr[0]
        val expectedR = qr[1].toInt()
    }

    init{initializeSimpleRecipPow10sDiv2()}
    val tcs = arrayOf(
        TC("3608658213331297855608970283708212065311874988898887037944571311754969355812", 5),
        TC("128527847997762162143568668656799570717260316632095375922895601761265673176", 5),
        TC("760800123124466166945293700104907906477961092165930023660968977", 1),
        TC("12345", 1),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        val expectedQ = tc.expectedQ
        val expectedR = tc.expectedR

        val (observedQ, observedR) = simpleRecipDivModPow10Div2(tc.bi, tc.pow10)

        assertEquals(expectedQ, observedQ)
        assertEquals(expectedR, observedR)
    }

    @Test
    fun testRandoms() {
        val randomMax = 100000
        for (i in 0..randomMax) {
            test1Random()
        }
    }

    val random = Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 256)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun test1Random() {
        val bi = randBi()
        val pow10 = random.nextInt(1, 6)
        val tc = TC(bi, pow10)
        test1(tc)
    }

}