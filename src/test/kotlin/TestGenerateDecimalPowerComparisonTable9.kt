package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.math.BigInteger

class TestGenerateDecimalPowerComparisonTable9 {

    @Test
    fun testPrintLimit() {
        val biMax64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)
        val biStr = biMax64.toString()
        val biStrLen = biStr.length
        val biStrHex = biMax64.toString(16)

        println("biMax64:$biStr ($biStrLen) $biStrHex")
    }

    fun getDigitCount(bitCount: Int) : Int {
        val biMax64 = BigInteger.ONE.shiftLeft(bitCount).subtract(BigInteger.ONE)
        val biStr = biMax64.toString()
        val biStrLen = biStr.length
        return biStrLen
    }

    @Test
    fun printBoundaryDigitCounts() {
        for (bitCount in 64..256 step 64) {
            val digitCount = getDigitCount(bitCount)
            println("bitCount:$bitCount  digitCount:$digitCount")
        }
    }

    @Test
    fun roundUpToPow10() {
        val bi = BigInteger.ONE.shiftLeft(16)
        val ceilLog10 = Math.ceil(Math.log10(bi.toDouble())).toInt()
        val biUp = BigInteger.TEN.pow(ceilLog10)
        println("bi:$bi  ceilLog10:$ceilLog10  biUp:$biUp")
    }

    @Test
    fun printTables() {
        for (bitCount in 64..256 step 64)
            printTable(bitCount - 64, bitCount)
    }

    @OptIn(ExperimentalStdlibApi::class)
    val numberHexFormat = HexFormat {
        upperCase = true
        number {
            removeLeadingZeros = true
            minLength = 16
            prefix = "0x"
        }
    }


    @OptIn(ExperimentalStdlibApi::class)
    fun printTableLine(bi: BigInteger) {
        val biStr = bi.toString()
        val biStrLen = biStr.length
        for (shift in 192 downTo 0 step 64) {
            val t = bi.shiftRight(shift)
            if (t.equals(BigInteger.ZERO) && shift != 0)
                continue
            val l = t.toLong()
            val hex = l.toHexString(numberHexFormat)
            print("${hex}uL.toLong(), ")
        }
        println("// $biStr ($biStrLen)")
    }

    fun printTable(minBitCount: Int, maxBitCount: Int) {
        val biMin = if (minBitCount == 0) BigInteger.ZERO else BigInteger.ONE.shiftLeft(minBitCount)
        val biMax = BigInteger.ONE.shiftLeft(maxBitCount).subtract(BigInteger.ONE)
        val minDigitCount = getDigitCount(minBitCount)
        val maxDigitCount = getDigitCount(maxBitCount)

        println("// minBitCount:$minBitCount  maxBitCount:$maxBitCount")
        //printTableLine(biMin)
        for (pow in minDigitCount..<maxDigitCount) {
            val biPow = BigInteger.TEN.pow(pow).subtract(BigInteger.ONE)
            printTableLine(biPow)
        }
        printTableLine(biMax)
    }
}