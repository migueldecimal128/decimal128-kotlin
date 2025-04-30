package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.*
import kotlin.math.ceil

class GenerateRmp05Correctly {
    companion object {

        val verbose = false

        // there can be errors here, but we are specifically testing and this will do for starters
        val rho = Math.log(10.0) / Math.log(2.0)
        val FIVE = 5.toBigInteger()

        fun calcTheoreticalMinY05(qDigitCount: Int, xPow10: Int): Int {
            val min10d = (qDigitCount + xPow10) * rho
            val min05 = ceil(min10d).toInt() - xPow10
            return min05
        }

        fun calcMinY05(qDigitCount:Int, xPow10:Int) : Int {
            val theoreticalMinY05 = calcTheoreticalMinY05(qDigitCount, xPow10)
            if (verbose)
                println("$qDigitCount $xPow10 => theory:$theoreticalMinY05")
            if (! verifyY05(qDigitCount, xPow10, theoreticalMinY05))
                throw RuntimeException("?que?")
            var minY05 = theoreticalMinY05
            while (verifyY05(qDigitCount, xPow10, minY05 - 1))
                --minY05
            return minY05
        }

        fun calcFivePowNegXScaled(xPow10:Int, yFractionalBitCount:Int) : BigInteger {
            val tenPowX = TEN.pow(xPow10)
            val fivePowX = tenPowX.shiftRight(xPow10)
            val fractionalScale = ONE.shiftLeft(yFractionalBitCount)
            val fivePowNegXScaled = fractionalScale.add(fivePowX).subtract(ONE).divide(fivePowX)
            return fivePowNegXScaled
        }

        fun verifyY05(qDigitCount: Int, xPow10: Int, yFractionalBitCount: Int): Boolean {
            val maxDividend10 = TEN.pow(qDigitCount)
            val maxDividend05 = maxDividend10.shiftRight(xPow10)
            val tenPowX = TEN.pow(xPow10)
            val fivePowX = tenPowX.shiftRight(xPow10)

            val actualMaxQuotientInteger = maxDividend10.divide(tenPowX)

            val pow10Mask = ONE.shiftLeft(xPow10).subtract(ONE)
            val pow10MaskShr1 = pow10Mask.shiftRight(1)
            val fractionalScale = ONE.shiftLeft(yFractionalBitCount)
            val fractionTailMask = fractionalScale.shiftRight(1).subtract(ONE)
            //val fivePowNegXScaled = fractionalScale.add(fivePowX).subtract(ONE).divide(fivePowX)
            val fivePowNegXScaled = calcFivePowNegXScaled(xPow10, yFractionalBitCount)

            fun verify05Even(d: BigInteger): Boolean {
                val actualQuotientInteger = d.divide(tenPowX)

                val d05 = d.shiftRight(xPow10 - 1)
                val fractionPow2 = d.and(pow10MaskShr1).toLong()

                val quotientScaled = d05.multiply(fivePowNegXScaled)
                val quotientIntegerAndRoundBit = quotientScaled.shiftRight(yFractionalBitCount)
                val quotientInteger = quotientIntegerAndRoundBit.shiftRight(1)
                if (!quotientInteger.equals(actualQuotientInteger))
                    return false;
                val roundBit = quotientIntegerAndRoundBit.toInt() and 1
                val fractionTail = quotientScaled.and(fractionTailMask)

                return roundBit == 0 && fractionPow2 == 0L && fractionTail < fivePowNegXScaled
            }

            fun verify05LtHalf(d: BigInteger): Boolean {
                val actualQuotientInteger = d.divide(tenPowX)

                val d05 = d.shiftRight(xPow10 - 1)
                val fractionPow2 = d.and(pow10MaskShr1).toLong()

                val quotientScaled = d05.multiply(fivePowNegXScaled)
                val quotientIntegerAndRoundBit = quotientScaled.shiftRight(yFractionalBitCount)
                val quotientInteger = quotientIntegerAndRoundBit.shiftRight(1)
                if (!quotientInteger.equals(actualQuotientInteger))
                    return false;
                val roundBit = quotientIntegerAndRoundBit.toInt() and 1
                val fractionTail = quotientScaled.and(fractionTailMask)

                return roundBit == 0 && (fractionPow2 != 0L || fractionTail >= fivePowNegXScaled)
            }

            fun verify05Half(d: BigInteger): Boolean {
                val actualQuotientInteger = d.divide(tenPowX)

                val d05 = d.shiftRight(xPow10 - 1)
                val fractionPow2 = d.and(pow10MaskShr1).toLong()

                val quotientScaled = d05.multiply(fivePowNegXScaled)
                val quotientIntegerAndRoundBit = quotientScaled.shiftRight(yFractionalBitCount)
                val quotientInteger = quotientIntegerAndRoundBit.shiftRight(1)
                if (!quotientInteger.equals(actualQuotientInteger))
                    return false;
                val roundBit = quotientIntegerAndRoundBit.toInt() and 1
                val fractionTail = quotientScaled.and(fractionTailMask)

                return roundBit == 1 && fractionPow2 == 0L && fractionTail < fivePowNegXScaled
            }

            fun verify05GtHalf(d: BigInteger): Boolean {
                val actualQuotientInteger = d.divide(tenPowX)

                val d05 = d.shiftRight(xPow10 - 1)
                val fractionPow2 = d.and(pow10MaskShr1).toLong()

                val quotientScaled = d05.multiply(fivePowNegXScaled)
                val quotientIntegerAndRoundBit = quotientScaled.shiftRight(yFractionalBitCount)
                val quotientInteger = quotientIntegerAndRoundBit.shiftRight(1)
                if (!quotientInteger.equals(actualQuotientInteger))
                    return false;
                val roundBit = quotientIntegerAndRoundBit.toInt() and 1
                val fractionTail = quotientScaled.and(fractionTailMask)

                return roundBit == 1 && (fractionPow2 != 0L || fractionTail >= fivePowNegXScaled)
            }

            val max = maxDividend10
            if (!verify05Even(max))
                return false

            val halfUlp = tenPowX.shiftRight(1)

            if (! verify05LtHalf(ONE))
                return false

            if (! verify05LtHalf(halfUlp.subtract(ONE)))
                return false

            if (!verify05Half(halfUlp))
                return false

            val min10 = TEN.pow(qDigitCount - 1)

            if (!verify05GtHalf(min10.subtract(ONE)))
                return false

            if (!verify05Even(min10))
                return false

            if (!verify05LtHalf(min10.add(ONE)))
                return false

            val min90 = max.subtract(min10)
            if (!verify05Even(min90))
                return false

            val min95 = min90.add(halfUlp)
            if (!verify05Half(min95))
                return false

            if (! verify05GtHalf(min95.add(ONE)))
                return false

            val min99 = max.subtract(ONE)
            if (!verify05GtHalf(min99))
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
    fun test70_40() {
        test(70, 40)
    }

    @Test
    fun test19_3() {
        test(19, 3)
    }

    fun test(q: Int, x: Int) {
        val yTheory = calcTheoreticalMinY05(q, x)
        val verifyTheory = verifyY05(q, x, yTheory)
        if (verbose)
            println("q:$q x:$x => yTheory:$yTheory verifyTheory:$verifyTheory")

        val yMin = calcMinY05(q, x)
        val verifyYMin = verifyY05(q, x, yMin)
        if (verbose)
            println("q:$q x:$x => y:$yTheory yMin:$yMin verifyYMin:$verifyYMin")


    }

    @Test
    fun test78_1() {
        test(78, 1)
        val yMin = calcMinY05(78, 1)
    }

    @Test
    fun test78_44() {
        test(78, 44)
        val yMin = calcMinY05(78, 44)
    }

    @Test
    fun genAll() {
        var maxMinY05 = 0
        var maxFivePowNegXScaled = ZERO
        for (qDigitCount in 2..78) { // include 78 digits ... up through 2**256-1
            for (xPow10 in 1..<Math.min(qDigitCount, 45)) {
                val theoreticalY05 = calcTheoreticalMinY05(qDigitCount, xPow10)
                val minY05 = calcMinY05(qDigitCount, xPow10)
                if (verbose)
                    println("q:$qDigitCount x:$xPow10 => theory:$theoreticalY05 -> min:$minY05")
                maxMinY05 = Math.max(maxMinY05, minY05)
                val fivePowNegXScaled = calcFivePowNegXScaled(xPow10, minY05)
                if (fivePowNegXScaled > maxFivePowNegXScaled)
                    maxFivePowNegXScaled = fivePowNegXScaled
            }
        }
        println("maxMinY05:$maxMinY05")
        println("maxFivePowNegXScaled:$maxFivePowNegXScaled bitLength:${maxFivePowNegXScaled.bitLength()}")
    }
}

