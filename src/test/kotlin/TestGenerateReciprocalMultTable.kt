package com.decimal128

import org.junit.jupiter.api.Test

import java.math.BigInteger
import kotlin.math.max

class TestGenerateReciprocalMultTable {

    data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    class RecipMultParams(val dividendDigitCount: Int, val divisorPow10: Int, val mul:BigInteger, val shift: Int, val accBitCount: Int, val quotBitCount: Int) {
        val mulBitCount = mul.bitLength()
        val mulWordCount get() = (mulBitCount + 31) / 32
        val accWordCount get() = (accBitCount + 31) / 32
        val quotWordCount get() = (quotBitCount + 31) / 32

        init {
            assert(mulWordCount <= 9) // 4 bits
            assert(accWordCount <= 17) // 5 bits
            assert(shift <= 404) // 9 bits
            assert(quotWordCount <= 8) // 4 bits
        }

        fun packHeader() = (mulWordCount) or (quotWordCount shl 4) or (accWordCount shl 8) or (shift shl 16)

        fun serialize(out: ArrayList<Int>) {
            out.add(packHeader())
            for (i in 0..<mulWordCount)
                out.add(mul.shiftRight(i * 32).toInt())
        }
    }

    val textLineLength = 100
    val params = arrayListOf(0)

    @Test
    fun testGenerate() {
        val longMax = Long.MAX_VALUE
        val longMaxLen = longMax.toString().length
        val sbLine = StringBuilder()
        val minQuotDigitCount = 2
        val maxQuotDigitCount = 78
        val minDivisorPow10 = 1
        val maxDivisorPow10 = 78-34
        val tableSize = (maxQuotDigitCount - minQuotDigitCount + 1) * (maxDivisorPow10 - minDivisorPow10 + 1)
        println("maxQuotDigitCount:$maxQuotDigitCount maxDivisionPow10:$maxDivisorPow10 sparseTableSize:$tableSize")
        var denseTableSize = 0
        var zeroTableCount = 0
        var maxMulWordCount = 0
        var maxAccWordCount = 0
        var maxShiftCount = 0
        var maxQuotWordCount = 0
        var strIndex = ""
        println("val minQuotDigitCount = $minQuotDigitCount")
        println("val maxQuotDigitCount = $maxQuotDigitCount")
        println("val minDivisorPow10 = $minDivisorPow10")
        println("val maxDivisorPow10 = $maxDivisorPow10")
        println("val recipMultIndexTable = shortArrayOf(")
        for (dividendDigitCount in minQuotDigitCount..maxQuotDigitCount) {
            for (divisorPow10 in 1..maxDivisorPow10) {
                val recipMultParams = generateRecipMultParams(dividendDigitCount, divisorPow10)
                if (recipMultParams.quotWordCount == 0) {
                    ++zeroTableCount
                    strIndex = "0,"
                } else {
                    ++denseTableSize
                    val index = serializeParams(recipMultParams)
                    strIndex = "$index,"
                }
                if (sbLine.length + strIndex.length > textLineLength) {
                    println(sbLine)
                    sbLine.clear()
                }
                sbLine.append(strIndex)
            }
        }
        sbLine.append(')')
        println(sbLine)
        println()
        println("val assertRecipMultIndexTableSize = run { assert(recipMultIndexTable.size == $tableSize) }")
        println()
        dumpSerializedParams()
        println()
        println("denseTableSize:$denseTableSize zeroTableCount:$zeroTableCount")
        println("maxMulWordCount:$maxMulWordCount maxAccWordCount:$maxAccWordCount maxShiftCount:$maxShiftCount maxQuotWordCount:$maxQuotWordCount")
    }

    fun generateParams(dividendDigitCount: Int, divisorPow10: Int, mul: BigInteger, mulWordCount: Int, accWordCount: Int, shift: Int, quotWordCount: Int) : Int {
        val index = params.size
        println("dividendDigits:$dividendDigitCount divisorPow10:$divisorPow10 mul:$mul ($mulWordCount word) accWordCount:$accWordCount shift:$shift quotWordCount:$quotWordCount")
        params.add(accWordCount)
        params.add(shift)
        params.add(quotWordCount)
        params.add(mulWordCount)
        for (i in 0..<mulWordCount)
            params.add(mul.shiftLeft(i * 32).toInt())
        return index
    }

    fun serializeParams(recipMultParams: RecipMultParams) : Int {
        val index = params.size
        recipMultParams.serialize(params)
        return index
    }

    fun dumpSerializedParams() {
        println("val recipMultParams = intArrayOf(")
        val sbLine = StringBuilder()
        for (i in params) {
            val str = "$i,"
            if (sbLine.length + str.length > textLineLength) {
                println(sbLine)
                sbLine.clear()
            }
            sbLine.append(str)
        }
        sbLine.append(')')
        println(sbLine)
        println()
        println("val assertRecipMultParamsSize = run { assert(recipMultParams.size == ${params.size}) }")

    }

    fun testGenerate1(dividendDigitCount: Int, divisorPow10: Int) : Quad<BigInteger, Int, Int, Int> {
        val biDividend = BigInteger.TEN.pow(dividendDigitCount).subtract(BigInteger.ONE)
        val biDivisor = BigInteger.TEN.pow(divisorPow10)
        //val biQuotient = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE)
        val (mul, shift) = generateMulAndShift(biDividend, biDivisor)
        val accumulatorBitCount = biDividend.multiply(mul).bitLength()
        val quotBitCount = biDividend.multiply(mul).shiftRight(shift).bitLength()
        return Quad(mul, shift, accumulatorBitCount, quotBitCount)
    }

    fun generateRecipMultParams(dividendDigitCount: Int, divisorPow10: Int) : RecipMultParams {
        val biDividend = BigInteger.TEN.pow(dividendDigitCount).subtract(BigInteger.ONE)
        val biDivisor = BigInteger.TEN.pow(divisorPow10)
        //val biQuotient = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE)
        val (mul, shift) = generateMulAndShift(biDividend, biDivisor)
        val accBitCount = biDividend.multiply(mul).bitLength()
        val quotBitCount = biDividend.multiply(mul).shiftRight(shift).bitLength()
        val recipMultParams = RecipMultParams(dividendDigitCount, divisorPow10, mul, shift, accBitCount, quotBitCount)
        return recipMultParams
    }

    fun generateMulAndShift(biMaxDividend: BigInteger, biDivisor: BigInteger) : Pair<BigInteger, Int> {
        for (shift in 1..1000) {
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


}