package com.decimal128

import com.decimal128.Residue.Companion.BIAS_TRUNC
import com.decimal128.Residue.Companion.EXACT
import com.decimal128.Residue.Companion.HALF
import java.math.BigInteger
import java.math.BigInteger.ZERO
import java.math.BigInteger.ONE
import java.math.BigInteger.TWO
import java.math.BigInteger.TEN
import kotlin.math.ceil

val MIN_DIVIDEND_DIGIT_COUNT = 2
val MAX_DIVIDEND_DIGIT_COUNT = 79 // exclusive
val MIN_DIVISOR_POW10 = 1
val MAX_DIVISOR_POW10 = MAX_DIVIDEND_DIGIT_COUNT - 34

class RecipMulPow10 {

    companion object {

        val THREE = 3.toBigInteger()

        val biMap = arrayOf(ZERO, ONE, TWO, THREE)

        class RecipMulParams5(val qDigitCount:Int, val xPow10:Int,
                              val fivePowNegXScaled:BigInteger, val yFractionalBitLength:Int) {

            val maxDividend = TEN.pow(qDigitCount)
            val maxProdWithRoundBit = maxDividend.shiftRight(xPow10 - 1).multiply(fivePowNegXScaled)
            val maxQuotRounded = maxProdWithRoundBit.shiftRight(yFractionalBitLength)

            val mulBitLength = fivePowNegXScaled.bitLength()
            val mulDwordLength get() = (mulBitLength + 63) / 64 // 3 bits
            val mulDigitLength = fivePowNegXScaled.toString().length
            val accBitLength = maxProdWithRoundBit.bitLength()
            val accDwordLength get() = (accBitLength + 63) / 64 // 4 bits
            val quotBitLength = maxQuotRounded.bitLength()
            val quotDwordLength get() = (quotBitLength + 63) / 64 // 3 bits

            // 3 bits mulDwordLength
            // 7 bits mulDigitLength
            // 4 bits accDwordLength
            // 9 bits yFractionalBitLength
            // 3 bits quotDwordLength

            fun packDescriptor() =
                ((mulDwordLength) or (mulDigitLength shl 3) or (accDwordLength shl 10) or
                        (yFractionalBitLength shl 14) or (quotDwordLength shl 23))

            fun serialize(out:ArrayList<Long>) {
                out.add(packDescriptor().toLong())
                for (i in 0..<mulDwordLength)
                    out.add(fivePowNegXScaled.shiftRight(i * 64).toLong())
            }

            override fun toString() :String {
                return "qDigitCount:$qDigitCount xPow10:$xPow10\n" +
                        "  mulDwordCount:$mulDwordLength mulBitCount:$mulBitLength mulDigitCount:$mulDigitLength\n" +
                        "  fivePowNegXScaled:$fivePowNegXScaled yFractionalBitCount:$yFractionalBitLength\n" +
                        "  accDwordCount:$accDwordLength accBitCount:$accBitLength\n" +
                        "  quotDwordCount:$quotDwordLength quotBitCount:$quotBitLength\n" +
                        ""

            }
        }

        fun serializeParams(params:ArrayList<Long>, recipMulParams: RecipMulParams5) : Int {
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
            if (params.quotBitLength > 0 && shift % 64 != 0) {
                // try rounding up to next 64-bit boundary
                val shift64 = ((shift + 63) / 64) * 64
                val (mul64, shiftT) = generateMulAndShift(biDividend5, biDivisor5, shift64)
                require(shiftT == shift64)
                val params64 = RecipMulParams5(dividendDigitCount, divisorPow10, mul64, shift64)
                require(params.quotBitLength == params64.quotBitLength)
                if ((params64.accDwordLength == params.accDwordLength) && (params64.mulDwordLength == params.mulDwordLength)) {
                    //    println("params:$params")
                    //    println("params64:$params64")
                    //    println("RoundUp!")
                    return params64
                }
            }
            return params
        }

        fun generateMulAndShift(
            biMaxDividend: BigInteger,
            biDivisor: BigInteger,
            startShift: Int
        ) : Pair<BigInteger, Int> {
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

        fun calcRecipMulParams5(qDigitCount:Int, xPow10:Int) : RecipMulParams5 {
            val yFractionalBitCount = calcMinY05(qDigitCount, xPow10)
            val fivePowNegXScaled = calcFivePowNegXScaled(xPow10, yFractionalBitCount)
            val rcmp5 = RecipMulParams5(qDigitCount, xPow10, fivePowNegXScaled, yFractionalBitCount)
            return rcmp5
        }


        var initialized = false

        val rowSize = MAX_DIVISOR_POW10 - MIN_DIVISOR_POW10
        val tableSize = (MAX_DIVIDEND_DIGIT_COUNT - MIN_DIVIDEND_DIGIT_COUNT) * rowSize

        val INDEXES = IntArray(tableSize)
        var PARAMS = LongArray(0)

        fun indexOf(digitCount: Int, pow10: Int) : Int {
            assert(digitCount in MIN_DIVIDEND_DIGIT_COUNT..<MAX_DIVIDEND_DIGIT_COUNT)
            assert(pow10 in MIN_DIVISOR_POW10..<MAX_DIVISOR_POW10)
            val index = (digitCount - MIN_DIVIDEND_DIGIT_COUNT) * rowSize + (pow10 - MIN_DIVISOR_POW10)
            return index
        }

        fun initialize() {
            if (initialized)
                return
            val paramsArrayList = ArrayList<Long>(tableSize * 4)
            paramsArrayList.add(0L)
            for (qDigitCount in MIN_DIVIDEND_DIGIT_COUNT..<MAX_DIVIDEND_DIGIT_COUNT) {
                val maxPow10 = Math.min(qDigitCount, MAX_DIVISOR_POW10)
                for (xPow10 in MIN_DIVISOR_POW10..<maxPow10) {
                    val index = indexOf(qDigitCount, xPow10)
                    if (qDigitCount > xPow10) {
                        val rmp5 = calcRecipMulParams5(qDigitCount, xPow10)
                        val paramsIndex = serializeParams(paramsArrayList, rmp5)
//                    println("digitCount10:$digitCount10 pow10:$pow10 $rmp5")
//                    println()
                        INDEXES[index] = paramsIndex
                    } else {
                        INDEXES[index] = 0
                    }
                }
            }
            PARAMS = paramsArrayList.toLongArray()
            println("initialized: params.size:${PARAMS.size}")
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
            for (digitCount in MIN_DIVIDEND_DIGIT_COUNT..<MAX_DIVIDEND_DIGIT_COUNT) {
                for (pow10 in MIN_DIVISOR_POW10..<MAX_DIVISOR_POW10) {
                    val index = indexOf(digitCount, pow10)
                    val paramsIndex = INDEXES[index]
                    if (paramsIndex == 0)
                        continue
                    val descriptor = PARAMS[paramsIndex].toInt()
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

        fun divModPow10(dividendDigitCount: Int, d0: Long, pow10: Int) : Pair<Long, Long> {
            throw RuntimeException("not impl")
        }

        fun divModPow10(q: Coeff, r: Coeff, d: Coeff, pow10: Int) {
            throw RuntimeException("not impl")
        }


        fun getMultShift(digitCount: Int, pow10: Int) : Pair<BigInteger, Int> {
            throw RuntimeException("not impl")
        }

        fun divPow10(q:Coeff, x:Coeff, pow10:Int, sign:Boolean, ctx:Decimal128Context) {
            initialize()
            if (pow10 <= 0) {
                assert(pow10 == 0)
                q.set(x)
                return
            }
            if (x.digitCount <= pow10) {
                if (x.digitCount == 0) {
                    q.setZero()
                    return
                }
                // otherwise, non-zero residue ... round it
                val residue = if (x.digitCount == pow10) Residue.residueFrom(x) else Residue.LT_HALF
                val roundUp = residue.ulpBias(ctx.roundingDirection.negate(sign), x.dw0)
                q.setLoBit(roundUp)
                ctx.setInexact()
                return
            }
            /*
            when {
                ((x.dw3 or x.dw2) == 0L) -> {
                    if (x.dw1 == 0L)
                        _divPow10(q, x.digitCount, x.dw0, pow10, sign, ctx)
                    else
                        _divPow10(q, x.digitCount, x.dw1, x.dw0, pow10, sign, ctx)
                }
                (x.dw3 == 0L) ->
                    _divPow10(q, x.digitCount, x.dw2, x.dw1, x.dw0, pow10, sign, ctx)
                else ->

             */
                    _divPow10(q, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0, pow10, sign, ctx)

            //}
        }

        private fun _divPow10(q:Coeff, xDigitCount:Int, x3:Long, x2:Long, x1:Long, x0:Long,
                              pow10:Int, sign:Boolean, ctx:Decimal128Context) {
            require(xDigitCount in MIN_DIVIDEND_DIGIT_COUNT..<MAX_DIVIDEND_DIGIT_COUNT)
            require(pow10 in MIN_DIVISOR_POW10..<MAX_DIVISOR_POW10)

            val index = indexOf(xDigitCount, pow10)
            val paramsIndex = INDEXES[index]
            if (paramsIndex == 0) {
                //println("don't forget to check for rounding in this case")
                throw RuntimeException("why am I here?")
            }
            val descriptor = PARAMS[paramsIndex].toInt()
            val mulDwordCount = unpackMulDwordCount(descriptor)
            val mulDigitCount = unpackMulDigitCount(descriptor)
            val accDwordCount = unpackAccDwordCount(descriptor)
            val shift = unpackShift(descriptor)
            val quotDwordCount = unpackQuotDwordCount(descriptor)
            val biMul = Ular.toBigInteger(PARAMS, paramsIndex + 1, mulDwordCount)

            val div = Ular.toBigInteger(x3, x2, x1, x0)

            val firstLoBits = if (pow10 == 1) ZERO else div.and(ONE.shiftLeft(pow10-1).subtract(ONE))
            val firstLoStickyBits = if (pow10 == 1 || div.and(ONE.shiftLeft(pow10-1).subtract(ONE)).equals(ZERO)) 0 else 1
            val dividend5 = div.shiftRight(pow10-1)
            val prod = dividend5.multiply(biMul)
            val prodBitLength = prod.bitLength()
            val prodDwordCount = (prodBitLength + 63) / 64
            val frac = prod.and(ONE.shiftLeft(shift).subtract(ONE))

            if (! (prodDwordCount <= accDwordCount)) {
                println("dividend:${div} pow10:${pow10}")
                println("prodDwordCount:$prodDwordCount accDwordCount:$accDwordCount")
            }
            assert(prodDwordCount <= accDwordCount)
            val quot5x2 = prod.shiftRight(shift) // the quotient*2 to get rounding bit
            val quot5x2BitLength = quot5x2.bitLength()
            val quot5x2DwordLength = (quot5x2BitLength + 63) / 64
            assert(quot5x2DwordLength <= quotDwordCount)
            //println("$div / 10**${tc.pow10} ==> quotRounded:$quotRounded")
            val quot5x2Lo2Bits = quot5x2.and(THREE).toInt()
            val residue =
                if (firstLoStickyBits == 0 && frac < biMul) {
                    if ((quot5x2Lo2Bits and 1) == 0) EXACT else HALF
                } else {
                    BIAS_TRUNC
                }
            val lsbIsOdd = (quot5x2Lo2Bits shr 1).toLong()

            val inexact = residue != EXACT
            val effectiveRoundingDirection = ctx.roundingDirection.negate(sign)
            val biasHalfUlp = residue.halfUlpBias(effectiveRoundingDirection, lsbIsOdd)
            val biBiasHalfUlp = biMap[biasHalfUlp.toInt()]

            val quotPlusHalfUlp = quot5x2.add(biBiasHalfUlp)
            val quotRounded = quotPlusHalfUlp.shiftRight(1)
            q.set(quotRounded)
            ctx.setInexact(inexact)
        }


    }


}
