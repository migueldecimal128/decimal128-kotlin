package com.decimal128

import org.junit.jupiter.api.Test

import java.math.BigInteger
import kotlin.math.max
import java.io.File
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.OutputStream

class TestGenerateReciprocalMultTable64 {

    class RecipMultParams(val dividendDigitCount: Int, val divisorPow10: Int, val mul:BigInteger, val shift: Int, val biDividend: BigInteger, val biDivisor: BigInteger) {
        val accBitCount = biDividend.multiply(mul).bitLength()
        val quotBitCount = biDividend.multiply(mul).shiftRight(shift).bitLength()
        val mulBitCount = mul.bitLength()
        val mulDwordCount get() = (mulBitCount + 63) / 64
        val accDwordCount get() = (accBitCount + 63) / 64
        val quotDwordCount get() = (quotBitCount + 63) / 64

        fun packHeader(headerIndex: Int) = ((mulDwordCount) or (quotDwordCount shl 8) or (accDwordCount shl 16)).toLong() or (shift.toLong() shl 32) or (headerIndex.toLong() shl 48)

        fun serialize(out: ArrayList<Long>) {
            val headerIndex = out.size
            out.add(packHeader(headerIndex))
            for (i in 0..<mulDwordCount)
                out.add(mul.shiftRight(i * 64).toLong())
        }

        override fun toString() : String {
            return "dividend:$dividendDigitCount divisor:$divisorPow10 mulDwordCount:$mulDwordCount mulBitCount:$mulBitCount " +
                    "shift:$shift accDwordCount:$accDwordCount accBitCount:$accBitCount quotDwordCount:$quotDwordCount quotBitCount:$quotBitCount"
        }
    }

    val textLineLength = 100
    val indexes = ArrayList<Int>(78 * (78 - 34))
    val params = arrayListOf(0L)

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
        var maxMulDwordCount = 0
        var maxAccDwordCount = 0
        var maxShiftCount = 0
        var maxQuotDwordCount = 0

        var accCounts = LongArray(10)
        var accShiftedCounts = LongArray(10)

        var noFractionalShiftCount = 0

        var strIndex = ""
        println("val minQuotDigitCount = $minQuotDigitCount")
        println("val maxQuotDigitCount = $maxQuotDigitCount")
        println("val minDivisorPow10 = $minDivisorPow10")
        println("val maxDivisorPow10 = $maxDivisorPow10")
        println("val recipMultIndexTable = shortArrayOf(")
        for (dividendDigitCount in minQuotDigitCount..maxQuotDigitCount) {
            for (divisorPow10 in 1..maxDivisorPow10) {
                val recipMultParams = generateRecipMultParams(dividendDigitCount, divisorPow10)
                if (recipMultParams.quotDwordCount == 0) {
                    ++zeroTableCount
                    indexes.add(0)
                } else {
                    println(recipMultParams)
                    ++denseTableSize
                    val index = serializeParams(recipMultParams)
                    indexes.add(index)
                    if (recipMultParams.mulDwordCount > maxMulDwordCount) {
                        println(">>> new maxMulDwordCount : $recipMultParams")
                    } else if (recipMultParams.mulDwordCount < maxMulDwordCount) {
                        println("mulDwordCount regression : $recipMultParams")
                    }
                    if (recipMultParams.accDwordCount > maxAccDwordCount) {
                        println("--> new accDwordCount : $recipMultParams")
                    } else if (recipMultParams.accDwordCount < maxAccDwordCount) {
                        println("accDwordCount regression : $recipMultParams")

                    }

                    maxMulDwordCount = max(maxMulDwordCount, recipMultParams.mulDwordCount)
                    maxAccDwordCount = max(maxAccDwordCount, recipMultParams.accDwordCount)
                    maxShiftCount = max(maxShiftCount, recipMultParams.shift)
                    maxQuotDwordCount = max(maxQuotDwordCount, recipMultParams.quotDwordCount)

                    ++accCounts[recipMultParams.accDwordCount]
                    ++accShiftedCounts[recipMultParams.accDwordCount - recipMultParams.shift/64]

                    if (recipMultParams.shift % 64 == 0)
                        ++noFractionalShiftCount
                }
            }
        }
        println()
        println("val assertRecipMultIndexTableSize = run { assert(recipMultIndexTable.size == $tableSize) }")
        println()

        val out = BufferedOutputStream(FileOutputStream(File("src/main/resources/recipMultPow10.bin")))
        dumpFileHeader(out, minQuotDigitCount, maxQuotDigitCount, minDivisorPow10, maxDivisorPow10)
        dumpIndexes(out)
        dumpSerializedParams(out)
        out.close()
        println()
        println("paramsSize:${params.size}")
        println("denseTableSize:$denseTableSize zeroTableCount:$zeroTableCount")
        println("noFractionalShiftCount:$noFractionalShiftCount")
        println("maxMulDwordCount:$maxMulDwordCount maxAccWordCount:$maxAccDwordCount maxShiftCount:$maxShiftCount maxQuotDwordCount:$maxQuotDwordCount")
        for (i in accCounts.indices)
            println("accCount $i ${accCounts[i]}   accShiftedCount:${accShiftedCounts[i]}")
    }

    fun serializeParams(recipMultParams: RecipMultParams) : Int {
        val index = params.size
        recipMultParams.serialize(params)
        return index
    }

    fun dumpFileHeader(out: OutputStream,
                       minQuotDigitCount: Int, maxQuotDigitCount: Int,
                       minDivisorPow10: Int, maxDivisorPow10: Int) {
        out.write('{'.code.toInt())
        out.write(minQuotDigitCount)
        out.write(maxQuotDigitCount)
        out.write(minDivisorPow10)
        out.write(maxDivisorPow10)
        out.write('}'.code.toInt())
    }

    fun dumpIndexes(out: OutputStream) {
        out.write('{'.code.toInt())
        val size = indexes.size
        out.write(size shr 8)
        out.write(size and 0xFF)
        out.write('='.code.toInt())
        for (index in indexes) {
            // write out as shorts
            out.write(index shr 8)
            out.write(index and 0xFF)
        }
        out.write('}'.code.toInt())
    }

    fun dumpSerializedParams(out: OutputStream) {
        val size = params.size
        out.write('{'.code.toInt())
        out.write(size shr 8)
        out.write(size and 0xFF)
        out.write('='.code.toInt())
        for (i in params) {
            out.write((i shr 56).toInt())
            out.write((i shr 48).toInt())
            out.write((i shr 40).toInt())
            out.write((i shr 32).toInt())
            out.write((i shr 24).toInt())
            out.write((i shr 16).toInt())
            out.write((i shr 8).toInt())
            out.write(i.toInt())
        }
        out.write('}'.code.toInt())
    }

    fun generateRecipMultParams(dividendDigitCount: Int, divisorPow10: Int) : RecipMultParams {
        val biDividend = BigInteger.TEN.pow(dividendDigitCount).subtract(BigInteger.ONE)
        val biDivisor = BigInteger.TEN.pow(divisorPow10)
        val (mul, shift) = generateMulAndShift(biDividend, biDivisor, 1)
        val params = RecipMultParams(dividendDigitCount, divisorPow10, mul, shift, biDividend, biDivisor)
        if (params.quotBitCount > 0 && shift % 64 != 0) {
            // try rounding up to next 64-bit boundary
            val shift64 = ((shift + 63) / 64) * 64
            val (mul64, shiftT) = generateMulAndShift(biDividend, biDivisor, shift64)
            require (shiftT == shift64)
            val params64 = RecipMultParams(dividendDigitCount, divisorPow10, mul64, shift64, biDividend, biDivisor)
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


}