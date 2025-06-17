package com.decimal128

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.*
import kotlin.math.min

class GenerateRmp10Correctly {
    companion object {

        val verbose = false

        // there can be errors here, but we are specifically testing and this will do for starters
        val rho = Math.log(10.0) / Math.log(2.0)
        val FIVE = 5.toBigInteger()

        fun calcTheoreticalMinY10(qDigitCount: Int, xPow10: Int): Int {
            val min10d = (qDigitCount + xPow10) * rho
            val min10 = Math.ceil(min10d).toInt()
            return min10
        }

        fun calcMinY10(qDigitCount:Int, xPow10:Int) : Int {
            val theoreticalMinY10 = calcTheoreticalMinY10(qDigitCount, xPow10)
            if (! verifyY(qDigitCount, xPow10, theoreticalMinY10))
                throw RuntimeException("?que?")
            var minY10 = theoreticalMinY10
            while (verifyY(qDigitCount, xPow10, minY10 - 1))
                --minY10
            return minY10
        }

        fun verifyY(qDigitCount: Int, xPow10: Int, yFractionalBitCount: Int): Boolean {
            val maxDividend10 = TEN.pow(qDigitCount)
            val tenPowX = TEN.pow(xPow10)

            val actualMaxQuotientInteger = maxDividend10.divide(tenPowX)
            val actualMaxQuotientInteger2 = TEN.pow(qDigitCount - xPow10)
            assertEquals(actualMaxQuotientInteger, actualMaxQuotientInteger2)

            val fractionalScale = ONE.shiftLeft(yFractionalBitCount)
            val fractionMask = fractionalScale.subtract(ONE)
            val roundBitMask = fractionalScale.shiftRight(1)
            val fractionTailMask = roundBitMask.subtract(ONE)
            val tenPowNegXScaled = fractionalScale.add(tenPowX).subtract(ONE).divide(tenPowX)


            fun verify10Even(d: BigInteger): Boolean {
                val actualQuotientInteger = d.divide(tenPowX)

                val quotientScaled = d.multiply(tenPowNegXScaled)
                val quotientInteger = quotientScaled.shiftRight(yFractionalBitCount)
                if (!quotientInteger.equals(actualQuotientInteger))
                    return false;
                val quotientFraction = quotientScaled.and(fractionMask)
                val roundBit = quotientFraction.and(roundBitMask).shiftRight(yFractionalBitCount - 1).toInt()
                val fractionTail = quotientFraction.and(fractionTailMask)

                return roundBit == 0 && fractionTail < tenPowNegXScaled
            }

            fun verify10LtHalf(d: BigInteger): Boolean {
                val actualQuotientInteger = d.divide(tenPowX)

                val quotientScaled = d.multiply(tenPowNegXScaled)
                val quotientInteger = quotientScaled.shiftRight(yFractionalBitCount)
                if (!quotientInteger.equals(actualQuotientInteger))
                    return false;
                val quotientFraction = quotientScaled.and(fractionMask)
                val roundBit = quotientFraction.and(roundBitMask).shiftRight(yFractionalBitCount - 1).toInt()
                val fractionTail = quotientFraction.and(fractionTailMask)

                return roundBit == 0 && fractionTail >= tenPowNegXScaled
            }

            fun verify10Half(d: BigInteger): Boolean {
                val actualQuotientInteger = d.divide(tenPowX)

                val quotientScaled = d.multiply(tenPowNegXScaled)
                val quotientInteger = quotientScaled.shiftRight(yFractionalBitCount)
                if (!quotientInteger.equals(actualQuotientInteger))
                    return false;
                val quotientFraction = quotientScaled.and(fractionMask)
                val roundBit = quotientFraction.and(roundBitMask).shiftRight(yFractionalBitCount - 1).toInt()
                val fractionTail = quotientFraction.and(fractionTailMask)

                return roundBit == 1 && fractionTail < tenPowNegXScaled
            }

            fun verify10GtHalf(d: BigInteger): Boolean {
                val actualQuotientInteger = d.divide(tenPowX)

                val quotientScaled = d.multiply(tenPowNegXScaled)
                val quotientInteger = quotientScaled.shiftRight(yFractionalBitCount)
                if (!quotientInteger.equals(actualQuotientInteger))
                    return false;
                val quotientFraction = quotientScaled.and(fractionMask)
                val roundBit = quotientFraction.and(roundBitMask).shiftRight(yFractionalBitCount - 1).toInt()
                val fractionTail = quotientFraction.and(fractionTailMask)

                return roundBit == 1 && fractionTail >= tenPowNegXScaled
            }

            val max = maxDividend10
            if (!verify10Even(max))
                return false

            val halfUlp = tenPowX.shiftRight(1)

            if (! verify10LtHalf(ONE))
                return false

            if (! verify10LtHalf(halfUlp.subtract(ONE)))
                return false

            if (!verify10Half(halfUlp))
                return false

            val min10 = TEN.pow(qDigitCount - 1)

            if (!verify10GtHalf(min10.subtract(ONE)))
                return false

            if (!verify10Even(min10))
                return false

            if (!verify10LtHalf(min10.add(ONE)))
                return false

            val min90 = max.subtract(min10)
            if (!verify10Even(min90))
                return false

            val min95 = min90.add(halfUlp)
            if (!verify10Half(min95))
                return false

            if (! verify10GtHalf(min95.add(ONE)))
                return false

            val min99 = max.subtract(ONE)
            if (!verify10GtHalf(min99))
                return false

            return true


        }
    }

    @Test
    fun test2_1() {
        test(2, 1)
    }

    @Test
    fun test3_2() {
        test(3, 2)
    }

    @Test
    fun test3_1() {
        test(3, 1)
    }

    @Test
    fun test19_3() {
        test(19, 3)
    }

    fun test(q: Int, x: Int) {
        var y = 0
        var verify = false
        y = calcTheoreticalMinY10(q, x)
        verify = verifyY(q, x, y)
        if (verbose)
            println("q:$q x:$x => y:$y verify:$verify")

        y -= 1
        verify = verifyY(q, x, y)
        if (verbose)
            println("q:$q x:$x => y:$y verify:$verify")


    }

    @Test
    fun genAll() {
        for (qDigitCount in 2..<78)
            for (xPow10 in 1..<min(qDigitCount, 45)) {
                val theoreticalY10 = calcTheoreticalMinY10(qDigitCount, xPow10)
                val minY10 = calcMinY10(qDigitCount, xPow10)
                if (verbose)
                    println("q:$qDigitCount x:$xPow10 => theory:$theoreticalY10 -> min:$minY10")
            }
    }
}

