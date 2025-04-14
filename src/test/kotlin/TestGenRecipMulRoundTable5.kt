package com.decimal128

import org.junit.jupiter.api.Test

import java.math.BigInteger
import kotlin.collections.ArrayList
import java.util.Random

import com.decimal128.Residual.Companion.EXACT
import com.decimal128.Residual.Companion.HALF
import com.decimal128.Residual.Companion.BIAS_TRUNC

val bi0 = BigInteger.ZERO
val bi1 = BigInteger.ONE
val bi3 = 3.toBigInteger()
val bi5 = 5.toBigInteger()
val bi10 = BigInteger.TEN




class TestGenRecipMulRoundTable5 {

    class RecipMulParams5(val dividendDigitCount: Int, val divisorPow10: Int,
                          val mul:BigInteger, val shift: Int) {
        val dividend1 = bi10.pow(dividendDigitCount-1)
        val dividend5 = dividend1.multiply(bi5)
        val dividend9 = bi10.pow(dividendDigitCount).subtract(bi1)
        val maxProd = dividend9.shiftRight(divisorPow10).multiply(mul)
        val accBitCount = maxProd.bitLength()
        val quotBitCount = maxProd.shiftRight(shift).bitLength()

        val mulBitCount = mul.bitLength()
        val mulDwordCount get() = (mulBitCount + 63) / 64 // 3 bits
        val mulDigitCount = mul.toString().length
        val accDwordCount get() = (accBitCount + 63) / 64 // 4 bits
        val quotDwordCount get() = (quotBitCount + 63) / 64 // 3 bits

        // 3 mulDwordCount
        // 7 mulDigitCount
        // 4 accDwordCount
        // 9 shift
        // 3 quotDwordCount

        fun packDescriptor() =
            ((mulDwordCount) or (mulDigitCount shl 3) or (accDwordCount shl 10) or
                    (shift shl 14) or (quotDwordCount shl 23))

        fun serialize(out: ArrayList<Long>) {
            out.add(packDescriptor().toLong())
            for (i in 0..<mulDwordCount)
                out.add(mul.shiftRight(i * 64).toLong())
        }

        override fun toString() : String {
            return "dividendDigitCount10:$dividendDigitCount divisorPow10:$divisorPow10\n" +
                    "  dividend1:$dividend1 dividend5:$dividend5 dividend9:$dividend9\n" +
                    "  mulDwordCount:$mulDwordCount mulBitCount:$mulBitCount mulDigitCount:$mulDigitCount\n" +
                    "  mul:$mul\n" +
                    "  accDwordCount:$accDwordCount accBitCount:$accBitCount shift:$shift\n" +
                    "  quotDwordCount:$quotDwordCount quotBitCount:$quotBitCount\n" +
                            ""

        }
    }

    var initialized = false

    val minDigitCount = 2
    val maxDigitCount = 79
    val minPow10 = 1
    val maxPow10 = maxDigitCount - 34
    val rowSize = maxPow10 - minPow10
    val tableSize = (maxDigitCount - minDigitCount) * rowSize

    val indexes = IntArray(tableSize)
    var params = LongArray(0)

    fun indexOf(digitCount: Int, pow10: Int) : Int {
        assert(digitCount in minDigitCount..<maxDigitCount)
        assert(pow10 in minPow10..<maxPow10)
        val index = (digitCount - minDigitCount) * rowSize + (pow10 - minPow10)
        return index
    }

    fun initialize() {
        if (initialized)
            return
        val paramsArrayList = ArrayList<Long>(tableSize * 4)
        paramsArrayList.add(0L)
        for (digitCount10 in minDigitCount..<maxDigitCount) {
            for (pow10 in minPow10..<maxPow10) {
                val index = indexOf(digitCount10, pow10)
                val rmp5 = generateRecipMulParams5(digitCount10, pow10)
                if (rmp5.quotDwordCount == 0) {
                    indexes[index] = 0
                } else {
                    val paramsIndex = serializeParams(paramsArrayList, rmp5)
//                    println("digitCount10:$digitCount10 pow10:$pow10 $rmp5")
//                    println()
                    indexes[index] = paramsIndex
                }
            }
        }
        params = paramsArrayList.toLongArray()
        println("initialized: params.size:${params.size}")
        initialized = true
    }

    // 3 mulDwordCount
    // 7 mulDigitCount
    // 4 accDwordCount
    // 9 shift
    // 3 quotDwordCount

    fun unpackMulDwordCount(rmp5Descriptor: Int) = (rmp5Descriptor ushr 0) and 0x07

    fun unpackMulDigitCount(rmp5Descriptor: Int) = (rmp5Descriptor ushr 3) and 0x07F

    fun unpackAccDwordCount(rmp5Descriptor: Int) = (rmp5Descriptor ushr 10) and 0x0F

    fun unpackShift(rmp5Descriptor: Int) = (rmp5Descriptor ushr 14) and 0x01FF

    fun unpackQuotDwordCount(rmp5Descriptor: Int) = (rmp5Descriptor ushr 23) and 0x07




    fun printStats() {
        var maxMulDwordCount = 0
        var maxMulDigitCount = 0
        var maxAccDwordCount = 0
        var maxShift = 0
        var maxQuotDwordCount = 0
        for (digitCount in minDigitCount..<maxDigitCount) {
            for (pow10 in minPow10..<maxPow10) {
                val index = indexOf(digitCount, pow10)
                val paramsIndex = indexes[index]
                if (paramsIndex == 0)
                    continue
                val descriptor = params[paramsIndex].toInt()
                val mulDwordCount = unpackMulDwordCount(descriptor)
                val mulDigitCount = unpackMulDigitCount(descriptor)
                val accDwordCount = unpackAccDwordCount(descriptor)
                val shift = unpackShift(descriptor)
                val quotDwordCount = unpackQuotDwordCount(descriptor)

                maxMulDwordCount = Math.max(maxMulDwordCount, mulDwordCount)
                maxMulDigitCount = Math.max(maxMulDigitCount, mulDigitCount)
                maxAccDwordCount = Math.max(maxAccDwordCount, accDwordCount)
                maxQuotDwordCount = Math.max(maxQuotDwordCount, quotDwordCount)
                maxShift = Math.max(maxShift, shift)
            }
        }
        println("maxMulDwordCount:$maxMulDwordCount maxMulDigitCount:$maxMulDigitCount")
        println("maxAccDwordCount:$maxAccDwordCount maxShift:$maxShift")
        println("maxQuotDwordCount:$maxQuotDwordCount")
    }

    @Test
    fun testGenerate() {
        initialize()
        printStats()
        println("TaDa!")
    }

    fun serializeParams(params: ArrayList<Long>, recipMulParams: RecipMulParams5) : Int {
        val paramsIndex = params.size
        recipMulParams.serialize(params)
        return paramsIndex
    }

    fun generateRecipMulParams5(dividendDigitCount: Int, divisorPow10: Int) : RecipMulParams5 {
        val biDividend10 = BigInteger.TEN.pow(dividendDigitCount).subtract(BigInteger.ONE)
        val biDividend5 = biDividend10.shr(divisorPow10)
        val biDivisor10 = BigInteger.TEN.pow(divisorPow10)
        val biDivisor5 = biDivisor10.shr(divisorPow10)
        val biQuotient10 = biDividend10.divide(biDivisor10)
        val biQuotient5 = biDividend5.divide(biDivisor5)
        assert(biQuotient10.equals(biQuotient5))
        val (mul, shift) = generateMulAndShift(biDividend5, biDivisor5, 1)
        val params = RecipMulParams5(dividendDigitCount, divisorPow10, mul, shift)
        if (params.quotBitCount > 0 && shift % 64 != 0) {
            // try rounding up to next 64-bit boundary
            val shift64 = ((shift + 63) / 64) * 64
            val (mul64, shiftT) = generateMulAndShift(biDividend5, biDivisor5, shift64)
            require (shiftT == shift64)
            val params64 = RecipMulParams5(dividendDigitCount, divisorPow10, mul64, shift64)
            require (params.quotBitCount == params64.quotBitCount)
            if ((params64.accDwordCount == params.accDwordCount) && (params64.mulDwordCount == params.mulDwordCount)) {
            //    println("params:$params")
            //    println("params64:$params64")
            //    println("RoundUp!")
                return params64
            }
        }
        return params
    }

    fun generateMulAndShift(biMaxDividend: BigInteger, biDivisor: BigInteger, startShift: Int) : Pair<BigInteger, Int> {
        for (shift in startShift..1000) {
            val mul = BigInteger.ONE.shiftLeft(shift).divide(biDivisor).add(BigInteger.ONE)
            val estimate = mul.multiply(biMaxDividend).shiftRight(shift)
            val actual = biMaxDividend.divide(biDivisor)
            if (tryMulAndShift(biMaxDividend, biDivisor, mul, shift)) {
                return mul to shift
            }
        }
        throw RuntimeException("fail")
    }

    fun tryMulAndShift(biQuotient: BigInteger, biDivisor: BigInteger, mul: BigInteger, shift: Int) : Boolean {
        val estimate = mul.multiply(biQuotient).shiftRight(shift)
        val actual = biQuotient.divide(biDivisor)
        return actual.equals(estimate)
    }

class TC(val dividend: BigInteger, val pow10: Int, val expectedResidual: Residual) {
        constructor(dividend: String, pow10: Int, residual: Residual) : this(BigInteger(dividend), pow10, residual)
        val quotient = dividend.divide(bi10.pow(pow10))
        val dividendDigitCount = dividend.toString().length
    }

    val tcs = arrayOf(
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
        initialize()
        for (tc in tcs)
            test1(tc)
    }

    val deltas = arrayOf(BigInteger.ONE.negate(), BigInteger.ZERO, BigInteger.ONE)
    val deltaRemStatuses = arrayOf(BIAS_TRUNC, EXACT, BIAS_TRUNC)

    @Test
    fun testBoundaries() {
        initialize()
        for (i in 2..77) {
            val biX = BigInteger.TEN.pow(i)
            for (pow10 in 0..44) {
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
        initialize()
        for (i in 0..<10000) {
            val bi = randBi()
            val pow10 = randPow(bi)
            val case = TC(bi, pow10, EXACT)
            test1(case)
        }

    }

    fun randPow(bi: BigInteger) : Int {
        //val biDigitCount = bi.toString().length
        //val maxPow = 78 - biDigitCount
        val maxPow = 45
        val randPow = random.nextInt(maxPow)
        return randPow
    }

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 256)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun test1(tc: TC) {
        if (tc.dividendDigitCount < minDigitCount) {
            println("dividendDigitCount ${tc.dividendDigitCount} too small")
            return
        }
        val index = indexOf(tc.dividendDigitCount, tc.pow10)
        val paramsIndex = indexes[index]
        if (paramsIndex == 0) {
            println("don't forget to check for rounding in this case")
            return
        }
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

        assert(prodDwordCount <= accDwordCount)
        val quot5x2 = prod.shiftRight(shift)
        val quot5x2BitLength = quot5x2.bitLength()
        val quot5x2DwordLength = (quot5x2BitLength + 63) / 64
        assert(quot5x2DwordLength <= quotDwordCount)
        val quotRounded =
            if (firstLoStickyBits == 0 && frac < biMul) {
                if (quot5x2.and(bi1).equals(bi0)) {
                    println("$div / ${tc.pow10} EXACT")
                    quot5x2.shiftRight(1)
                } else {
                    if (quot5x2.shiftRight(1).and(bi1).equals(bi0)) {
                        println("$div / ${tc.pow10} HALF even")
                        quot5x2.shiftRight(1)
                    } else {
                        println("$div / ${tc.pow10} HALF odd RoundUp")
                        quot5x2.plus(bi1).shiftRight(1)
                    }
                }
            } else {
                println("just round it up and truncate")
                quot5x2.plus(bi1).shiftRight(1)
            }
        println("$div / 10**${tc.pow10} ==> quotRounded:$quotRounded")
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
        println("$div / 10**${tc.pow10} ==> bitsQuot:$bitsQuot inexact:$inexact roundUp:$roundUp")

        if (firstLoStickyBits == 0 && frac < biMul) {
            if (quot5x2.and(bi1).equals(bi0)) {
                println("$div / ${tc.pow10} EXACT")
                quot5x2.shiftRight(1)
            } else {
                if (quot5x2.shiftRight(1).and(bi1).equals(bi0)) {
                    println("$div / ${tc.pow10} HALF even")
                    quot5x2.shiftRight(1)
                } else {
                    println("$div / ${tc.pow10} HALF odd RoundUp")
                    quot5x2.plus(bi1).shiftRight(1)
                }
            }
        } else {
            println("just round it up and truncate")
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

        println("Residual.inexactAndRoundupFrom(RoundingDirection.roundTiesToEven, $residual, $lsbIsOdd) => inexact2:$inexact2 bias:$bias")


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

