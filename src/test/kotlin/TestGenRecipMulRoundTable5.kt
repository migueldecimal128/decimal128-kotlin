package com.decimal128

import org.junit.jupiter.api.Test

import java.math.BigInteger
import kotlin.collections.ArrayList
import java.util.Random
import com.decimal128.RecipMulPow10.Companion.RecipMulParams5
import com.decimal128.RecipMulPow10.Companion.unpackShift
import com.decimal128.RecipMulPow10.Companion.unpackMulDigitCount
import com.decimal128.RecipMulPow10.Companion.unpackMulDwordCount
import com.decimal128.RecipMulPow10.Companion.unpackAccDwordCount
import com.decimal128.RecipMulPow10.Companion.unpackQuotDwordCount

import com.decimal128.Residual.Companion.EXACT
import com.decimal128.Residual.Companion.HALF
import com.decimal128.Residual.Companion.BIAS_TRUNC

class TestGenRecipMulRoundTable5 {

    companion object {
        val bi0 = BigInteger.ZERO
        val bi1 = BigInteger.ONE
        val bi3 = 3.toBigInteger()
        val bi5 = 5.toBigInteger()
        val bi10 = BigInteger.TEN
    }

    @Test
    fun testGenerate() {
        RecipMulPow10.initialize()
        RecipMulPow10.printStats()
        println("TaDa!")
    }

    class TC(val dividend: BigInteger, val pow10: Int, val expectedResidual: Residual) {
        constructor(dividend: String, pow10: Int, residual: Residual) : this(BigInteger(dividend), pow10, residual)
        val quotient = dividend.divide(bi10.pow(pow10))
        val dividendDigitCount = dividend.toString().length
    }

    val tcs = arrayOf(
        TC("9999999999", 1, BIAS_TRUNC),
        TC("10", 1, EXACT),
        TC("11", 1, BIAS_TRUNC),
        TC("14", 1, BIAS_TRUNC),
        TC("15", 1, HALF),
        TC("16", 1, BIAS_TRUNC),
        TC("19", 1, BIAS_TRUNC),
        TC("20", 1, EXACT),
        TC("21", 1, EXACT),
        TC("24", 1, EXACT),
        TC("25", 1, EXACT),
        TC("26", 1, EXACT),
        TC("29", 1, EXACT),
        TC("30", 1, EXACT),
        TC("100", 2, EXACT),
        TC("110", 2, BIAS_TRUNC),
        TC("120", 2, BIAS_TRUNC),
        TC("130", 2, BIAS_TRUNC),
        TC("140", 2, BIAS_TRUNC),
        TC("150", 2, HALF),
        TC("160", 2, BIAS_TRUNC),
        TC("170", 2, BIAS_TRUNC),
        TC("180", 2, BIAS_TRUNC),
        TC("190", 2, BIAS_TRUNC),
        TC("200", 2, BIAS_TRUNC),
        TC("210", 2, BIAS_TRUNC),
        TC("220", 2, BIAS_TRUNC),
        TC("230", 2, BIAS_TRUNC),
        TC("240", 2, BIAS_TRUNC),
        TC("250", 2, BIAS_TRUNC),
        TC("260", 2, BIAS_TRUNC),
        TC("270", 2, BIAS_TRUNC),
        TC("280", 2, BIAS_TRUNC),
        TC("290", 2, BIAS_TRUNC),
        TC("300", 2, BIAS_TRUNC),
        TC("310", 2, BIAS_TRUNC),
        TC("320", 2, BIAS_TRUNC),
        TC("330", 2, BIAS_TRUNC),
        TC("340", 2, BIAS_TRUNC),
        TC("350", 2, BIAS_TRUNC),
        TC("360", 2, BIAS_TRUNC),
        TC("370", 2, BIAS_TRUNC),
        TC("380", 2, BIAS_TRUNC),
        TC("390", 2, BIAS_TRUNC),
        TC("900", 2, BIAS_TRUNC),
        TC("910", 2, BIAS_TRUNC),
        TC("920", 2, BIAS_TRUNC),
        TC("930", 2, BIAS_TRUNC),
        TC("940", 2, BIAS_TRUNC),
        TC("950", 2, BIAS_TRUNC),
        TC("960", 2, BIAS_TRUNC),
        TC("970", 2, BIAS_TRUNC),
        TC("980", 2, BIAS_TRUNC),
        TC("990", 2, BIAS_TRUNC),
    )

    @Test
    fun testCases() {
        RecipMulPow10.initialize()
        for (tc in tcs)
            test1(tc)
    }

    val deltas = arrayOf(BigInteger.ONE.negate(), BigInteger.ZERO, BigInteger.ONE)
    val deltaRemStatuses = arrayOf(BIAS_TRUNC, EXACT, BIAS_TRUNC)

    @Test
    fun testBoundaries() {
        RecipMulPow10.initialize()
        for (i in 77..<78) {
            val biX = BigInteger.TEN.pow(i)
            for (pow10 in 1..<44) {
                for (j in deltas.indices) {
                    val deltaX = deltas[j]
                    val deltaRemStatus = deltaRemStatuses[j]
                    val biA = biX.add(deltaX)
                    val tc = TC(biA, pow10, deltaRemStatus)
                    test1(tc)
                }
            }
        }
    }

    val random = Random()

    @Test
    fun testRandomMul() {
        RecipMulPow10.initialize()
        for (i in 0..<10000) {
            val bi = randBi()
            val pow10 = randPow(bi)
            val case = TC(bi, pow10, EXACT)
            test1(case)
        }

    }

    fun randPow(bi: BigInteger) : Int {
        val biDigitCount = bi.toString().length
        val maxPow = MAX_DIVIDEND_DIGIT_COUNT - biDigitCount
        val randPow = random.nextInt(maxPow)
        return randPow
    }

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 256)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun test1(tc: TC) {
        if (tc.dividendDigitCount < MIN_DIVIDEND_DIGIT_COUNT || tc.dividendDigitCount >= MAX_DIVIDEND_DIGIT_COUNT ||
            tc.pow10 < MIN_DIVISOR_POW10 || tc.pow10 >= MAX_DIVISOR_POW10) {
            println("dividendDigitCount ${tc.dividendDigitCount} ${tc.pow10} out of range")
            return
        }
        val index = RecipMulPow10.indexOf(tc.dividendDigitCount, tc.pow10)
        val paramsIndex = RecipMulPow10.indexesPow5[index]
        if (paramsIndex == 0) {
            //println("don't forget to check for rounding in this case")
            return
        }
        val params = RecipMulPow10.paramsPow5
        val descriptor = params[paramsIndex].toInt()
        val mulDwordCount = unpackMulDwordCount(descriptor)
        val mulDigitCount = unpackMulDigitCount(descriptor)
        val accDwordCount = unpackAccDwordCount(descriptor)
        val shift = unpackShift(descriptor)
        val quotDwordCount = unpackQuotDwordCount(descriptor)
        val biMul = Ular.toBigInteger(params, paramsIndex + 1, mulDwordCount)

        val div = tc.dividend

        val firstLoBits = if (tc.pow10 == 1) bi0 else div.and(bi1.shiftLeft(tc.pow10-1).subtract(bi1))
        val firstLoStickyBits = if (tc.pow10 == 1 || div.and(bi1.shiftLeft(tc.pow10-1).subtract(bi1)).equals(bi0)) 0 else 1
        val dividend5 = div.shiftRight(tc.pow10-1)
        val prod = dividend5.multiply(biMul)
        val prodBitLength = prod.bitLength()
        val prodDwordCount = (prodBitLength + 63) / 64
        val frac = prod.and(bi1.shiftLeft(shift).subtract(bi1))

        if (! (prodDwordCount <= accDwordCount)) {
            println("dividend:${tc.dividend} pow10:${tc.pow10}")
            println("prodDwordCount:$prodDwordCount accDwordCount:$accDwordCount")
        }
        assert(prodDwordCount <= accDwordCount)
        val quot5x2 = prod.shiftRight(shift)
        val quot5x2BitLength = quot5x2.bitLength()
        val quot5x2DwordLength = (quot5x2BitLength + 63) / 64
        assert(quot5x2DwordLength <= quotDwordCount)
        val quotRounded =
            if (firstLoStickyBits == 0 && frac < biMul) {
                if (quot5x2.and(bi1).equals(bi0)) {
                    //println("$div / ${tc.pow10} EXACT")
                    quot5x2.shiftRight(1)
                } else {
                    if (quot5x2.shiftRight(1).and(bi1).equals(bi0)) {
                      //  println("$div / ${tc.pow10} HALF even")
                        quot5x2.shiftRight(1)
                    } else {
                       // println("$div / ${tc.pow10} HALF odd RoundUp")
                        quot5x2.plus(bi1).shiftRight(1)
                    }
                }
            } else {
                //println("just round it up and truncate")
                quot5x2.plus(bi1).shiftRight(1)
            }
        //println("$div / 10**${tc.pow10} ==> quotRounded:$quotRounded")
        val quot5x2Lo2Bits = quot5x2.and(bi3).toInt()
        val inexactAndRoundup =
            if (firstLoStickyBits == 0 && frac < biMul) {
                if ((quot5x2Lo2Bits and 1) == 0) {
                    0
                } else {
                    2 or (quot5x2Lo2Bits shr 1)
                }
            } else {
                3
            }

        val inexact = inexactAndRoundup shr 1
        val roundUp = inexactAndRoundup and 1
        val bitsQuot = quot5x2.plus(BigInteger(roundUp.toString())).shiftRight(1)
        //println("$div / 10**${tc.pow10} ==> bitsQuot:$bitsQuot inexact:$inexact roundUp:$roundUp")

        if (firstLoStickyBits == 0 && frac < biMul) {
            if (quot5x2.and(bi1).equals(bi0)) {
                //println("$div / ${tc.pow10} EXACT")
                quot5x2.shiftRight(1)
            } else {
                if (quot5x2.shiftRight(1).and(bi1).equals(bi0)) {
                  //  println("$div / ${tc.pow10} HALF even")
                    quot5x2.shiftRight(1)
                } else {
                    //println("$div / ${tc.pow10} HALF odd RoundUp")
                    quot5x2.plus(bi1).shiftRight(1)
                }
            }
        } else {
            //println("just round it up and truncate")
            quot5x2.plus(bi1).shiftRight(1)
        }

        val residual =
            if (firstLoStickyBits == 0 && frac < biMul) {
                if ((quot5x2Lo2Bits and 1) == 0) EXACT else HALF
            } else {
                if ((quot5x2Lo2Bits and 1) == 0) BIAS_TRUNC else BIAS_TRUNC
            }
        val lsbIsOdd = (quot5x2Lo2Bits shr 1).toLong()

        val inexact2 = residual != EXACT
        val bias = Residual.biasFrom(RoundingDirection.ROUND_TIES_TO_EVEN, residual, lsbIsOdd)

        //println("Residual.inexactAndRoundupFrom(RoundingDirection.roundTiesToEven, $residual, $lsbIsOdd) => inexact2:$inexact2 bias:$bias")


        val pause = 1

    //    assertEquals(tc.quotient, quot5)
        /*
        // now figure out rounding
        val cmpHalf = frac.compareTo(biHalfFrac)
        val remStatus =
        if (cmpHalf < 0) {
            if (loBits2.equals(bi0) && frac.equals(biExactFrac))
                RemainderStatus.EXACT
            else
                RemainderStatus.BIAS_TRUNC
        } else if (cmpHalf == 0 && loBits2.equals(bi0))
            RemainderStatus.HALF
        else
            RemainderStatus.BIAS_TRUNC

        assertEquals(tc.expectedRemainderStatus, remStatus)

         */
    }

}

