@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul4
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul3
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul2
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul1
import com.decimal128.DivBarrett.barrettDivPow10_32_128
import com.decimal128.DivBarrett.barrettDivPow10_32_192
import com.decimal128.DivBarrett.barrettDivPow10_32_256
import com.decimal128.DivBarrett.barrettDivPow10_50_114
import com.decimal128.DivBarrett.barrettDivPow10_50_164
import com.decimal128.DivBarrett.barrettDivPow10_50_214
import com.decimal128.DivBarrett.barrettDivPow10_50_256
import com.decimal128.DivBarrett.barrettDivPow10_64
import java.lang.Long.compareUnsigned
import java.lang.Math.unsignedMultiplyHigh
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

object RecipMulPow10 {

    val THREE = BigInteger.valueOf(3)
    val FIVE = BigInteger.valueOf(5)

    val biMap = arrayOf(ZERO, ONE, TWO, THREE)

    class RecipMulParams5(
        val qDigitCount: Int, val xPow10: Int,
        val fivePowNegXScaled: BigInteger, val yFractionalBitLength: Int
    ) {

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

        fun serialize(out: ArrayList<Long>) {
            out.add(packDescriptor().toLong())
            for (i in 0..<mulDwordLength)
                out.add(fivePowNegXScaled.shiftRight(i * 64).toLong())
        }

        override fun toString(): String {
            return "qDigitCount:$qDigitCount xPow10:$xPow10\n" +
                    "  mulDwordCount:$mulDwordLength mulBitCount:$mulBitLength mulDigitCount:$mulDigitLength\n" +
                    "  fivePowNegXScaled:$fivePowNegXScaled yFractionalBitCount:$yFractionalBitLength\n" +
                    "  accDwordCount:$accDwordLength accBitCount:$accBitLength\n" +
                    "  quotDwordCount:$quotDwordLength quotBitCount:$quotBitLength\n" +
                    ""

        }
    }

    fun serializeParams(params: ArrayList<Long>, recipMulParams: RecipMulParams5): Int {
        val paramsIndex = params.size
        recipMulParams.serialize(params)
        return paramsIndex
    }

    fun generateRecipMulParams5(dividendDigitCount: Int, divisorPow10: Int): RecipMulParams5 {
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
    ): Pair<BigInteger, Int> {
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

    fun tryMulAndShift(biQuotient: BigInteger, biDivisor: BigInteger, mul: BigInteger, shift: Int): Boolean {
        val estimate = mul.multiply(biQuotient).shiftRight(shift)
        val actual = biQuotient.divide(biDivisor)
        return actual.equals(estimate)
    }

    // there can be errors here, but we are specifically testing and this will do for starters
    val rho = Math.log(10.0) / Math.log(2.0)

    fun calcTheoreticalMinY05(qDigitCount: Int, xPow10: Int): Int {
        val min10d = (qDigitCount + xPow10) * rho
        val min05 = ceil(min10d).toInt() - xPow10
        return min05
    }

    fun calcMinY05(qDigitCount: Int, xPow10: Int): Int {
        val theoreticalMinY05 = calcTheoreticalMinY05(qDigitCount, xPow10)
        if (!verifyY05(qDigitCount, xPow10, theoreticalMinY05))
            throw RuntimeException("?que?")
        var minY05 = theoreticalMinY05
        while (verifyY05(qDigitCount, xPow10, minY05 - 1))
            --minY05
        return minY05
    }

    fun calcFivePowNegXScaled(xPow10: Int, yFractionalBitCount: Int): BigInteger {
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

        if (!verify05LtHalf(ONE))
            return false

        if (!verify05LtHalf(halfUlp.subtract(ONE)))
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

        if (!verify05GtHalf(min95.add(ONE)))
            return false

        val min99 = max.subtract(ONE)
        if (!verify05GtHalf(min99))
            return false

        return true


    }

    fun calcRecipMulParams5(qDigitCount: Int, xPow10: Int): RecipMulParams5 {
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

    fun indexOf(digitCount: Int, pow10: Int): Int {
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

    fun divModPow10(dividendDigitCount: Int, d0: Long, pow10: Int): Pair<Long, Long> {
        throw RuntimeException("not impl")
    }

    fun divModPow10(q: Coeff, r: Coeff, d: Coeff, pow10: Int) {
        throw RuntimeException("not impl")
    }


    fun getMultShift(digitCount: Int, pow10: Int): Pair<BigInteger, Int> {
        throw RuntimeException("not impl")
    }

    fun divPow10(z: Coeff, x: Coeff, pow10: Int): Residue {
        val xBitLen = x.bitLen
        if (pow10 < BARRETT_POW10_MAX)
            return DivBarrett.barrettDivPow10(z, x, pow10)
        if (pow10 < MAGIC_POW10_MAX && xBitLen <= MAGIC_MAX_DIVISOR_BITLEN) {
            return DivMagic.magicDivPow10(z, x, pow10)
        }
        initialize()
        val xDigitLen = x.digitLen
        if (xDigitLen <= pow10) {
            if (xDigitLen == 0) {
                z.coeffSetZero()
                return EXACT
            }
            val residue = if (xDigitLen == pow10) Residue.residueFrom(x) else Residue.LT_HALF
            return residue
        }
        return _divPow10(z, xDigitLen, x.dw3, x.dw2, x.dw1, x.dw0, pow10)
    }

    fun divPow10(q: Coeff, x: Coeff, pow10: Int, sign: Boolean, ctx: Decimal128Context) {
        initialize()
        if (pow10 <= 0) {
            assert(pow10 == 0)
            q.coeffSet(x)
            return
        }
        if (x.digitLen <= pow10) {
            if (x.digitLen == 0) {
                q.coeffSetZero()
                return
            }
            // otherwise, non-zero residue ... round it
            val residue = if (x.digitLen == pow10) Residue.residueFrom(x) else Residue.LT_HALF
            val roundUp = residue.ulpBias(ctx.roundingDirection.negate(sign), x.dw0)
            q.setZeroOrOneMasked(roundUp)
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
        _divPow10(q, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0, pow10, sign, ctx)

        //}
    }

    private fun _divPow10(
        q: Coeff, xDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long,
        pow10: Int) =
        _divPow10_miguel3(q, xDigitCount, x3, x2, x1, x0, pow10)

    private fun _divPow10(
        q: Coeff, xDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long,
        pow10: Int, sign: Boolean, ctx: Decimal128Context
    ) =
        _divPow10_miguel3(q, xDigitCount, x3, x2, x1, x0, pow10, sign, ctx)

    private fun _divPow10_miguel3(
        q: Coeff, xDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long,
        pow10: Int, sign: Boolean, ctx: Decimal128Context
    ) {
        require(xDigitCount in MIN_DIVIDEND_DIGIT_COUNT..<MAX_DIVIDEND_DIGIT_COUNT)
        require(pow10 in MIN_DIVISOR_POW10..<MAX_DIVISOR_POW10)
        // clear coeff without worrying about aliasing
        q.coeffEnableIndexSetAndZeroOut()

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
        val fractionBitLen = shift + 1 // include the halfUlp bit
        val quotDwordCount = unpackQuotDwordCount(descriptor)
        assert(quotDwordCount <= 5)

        val dividendShiftRight = pow10 - 1
        val dividendShiftLeft = -dividendShiftRight
        val shiftNonZeroMask = if (dividendShiftRight == 0) 0L else -1L
        val stickyBitsPow2 = x0 and shiftNonZeroMask and ((1L shl dividendShiftRight) - 1)

        val d0 = ((x1 shl dividendShiftLeft) and shiftNonZeroMask) or (x0 ushr dividendShiftRight)
        val d1 = ((x2 shl dividendShiftLeft) and shiftNonZeroMask) or (x1 ushr dividendShiftRight)
        val d2 = ((x3 shl dividendShiftLeft) and shiftNonZeroMask) or (x2 ushr dividendShiftRight)
        val d3 = (x3 ushr dividendShiftRight)

        val residue = when {
            (d3 != 0L) ->
                coeffRecipMul4(
                    q, PARAMS, paramsIndex + 1, mulDwordCount,
                    d3, d2, d1, d0, fractionBitLen, stickyBitsPow2
                )

            (d2 != 0L) ->
                coeffRecipMul3(
                    q, PARAMS, paramsIndex + 1, mulDwordCount,
                    d2, d1, d0, fractionBitLen, stickyBitsPow2
                )

            (d1 != 0L) ->
                coeffRecipMul2(
                    q, PARAMS, paramsIndex + 1, mulDwordCount,
                    d1, d0, fractionBitLen, stickyBitsPow2
                )

            (d0 != 0L) ->
                coeffRecipMul1(
                    q, PARAMS, paramsIndex + 1, mulDwordCount,
                    d0, fractionBitLen, stickyBitsPow2
                )

            else -> throw RuntimeException("why am I here?")
        }

        val effectiveRoundingDirection = ctx.roundingDirection.negate(sign)
        val ulpRoundUp = residue.ulpRoundUp(effectiveRoundingDirection, q.dw0)
        q.coeffIncrement(ulpRoundUp)

        q.coeffDisableIndexSetAndUpdateLengths()
        val inexact = residue != EXACT
        ctx.setInexact(inexact)
    }

    private fun _divPow10_miguel3(
        z: Coeff, xDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long,
        pow10: Int): Residue {
        require(xDigitCount in MIN_DIVIDEND_DIGIT_COUNT..<MAX_DIVIDEND_DIGIT_COUNT)
        require(pow10 in MIN_DIVISOR_POW10..<MAX_DIVISOR_POW10)
        // clear coeff without worrying about aliasing
        z.coeffEnableIndexSetAndZeroOut()

        val index = indexOf(xDigitCount, pow10)
        val paramsIndex = INDEXES[index]
        if (paramsIndex == 0) {
            //FIXME don't forget to check for rounding in this case ?
            throw RuntimeException("why am I here?")
        }
        val descriptor = PARAMS[paramsIndex].toInt()
        val mulDwordCount = unpackMulDwordCount(descriptor)
        val mulDigitCount = unpackMulDigitCount(descriptor)
        val accDwordCount = unpackAccDwordCount(descriptor)
        val shift = unpackShift(descriptor)
        val fractionBitLen = shift + 1 // include the halfUlp bit
        val quotDwordCount = unpackQuotDwordCount(descriptor)
        assert(quotDwordCount <= 5)

        val dividendShiftRight = pow10 - 1
        val dividendShiftLeft = -dividendShiftRight
        val shiftNonZeroMask = if (dividendShiftRight == 0) 0L else -1L
        val stickyBitsPow2 = x0 and shiftNonZeroMask and ((1L shl dividendShiftRight) - 1)

        val d0 = ((x1 shl dividendShiftLeft) and shiftNonZeroMask) or (x0 ushr dividendShiftRight)
        val d1 = ((x2 shl dividendShiftLeft) and shiftNonZeroMask) or (x1 ushr dividendShiftRight)
        val d2 = ((x3 shl dividendShiftLeft) and shiftNonZeroMask) or (x2 ushr dividendShiftRight)
        val d3 = (x3 ushr dividendShiftRight)

        val residue = when {
            (d3 != 0L) ->
                coeffRecipMul4(
                    z, PARAMS, paramsIndex + 1, mulDwordCount,
                    d3, d2, d1, d0, fractionBitLen, stickyBitsPow2
                )

            (d2 != 0L) ->
                coeffRecipMul3(
                    z, PARAMS, paramsIndex + 1, mulDwordCount,
                    d2, d1, d0, fractionBitLen, stickyBitsPow2
                )

            (d1 != 0L) ->
                coeffRecipMul2(
                    z, PARAMS, paramsIndex + 1, mulDwordCount,
                    d1, d0, fractionBitLen, stickyBitsPow2
                )

            (d0 != 0L) ->
                coeffRecipMul1(
                    z, PARAMS, paramsIndex + 1, mulDwordCount,
                    d0, fractionBitLen, stickyBitsPow2
                )

            else -> throw RuntimeException("why am I here?")
        }

        z.coeffDisableIndexSetAndUpdateLengths()
        return residue
    }

    private inline fun _magicDivide1x1(
        q: Coeff,
        x0: Long,
        m: Long,
        flagShift: Int,
    ): Residue {
        val s = flagShift and 0x3F
        val qLostCarry = 1L shl -s
        val addMask = (flagShift shr 31).toLong()
        val pp00Hi = unsignedMultiplyHigh(x0, m)
        val pp00Lo = x0 * m
        val p0 = pp00Lo
        val p1 = pp00Hi
        val qLo = p1 + (x0 and addMask)
        val qCarryAdd = if (compareUnsigned(qLo, p1) < 0) qLostCarry else 0L
        val q0 = qCarryAdd + (qLo ushr s)
        q.coeffSet64(q0)

        val roundBit = (p1 shr (s - 1)).toInt() and 1
        val cmpLo = compareUnsigned(p0, m)
        val hiFracMask = (1L shl (s - 1)) - 1L
        val stickyBit = if ((cmpLo >= 0) or ((p1 and hiFracMask) != 0L)) 1 else 0
        val residue = Residue.residueFrom(roundBit, stickyBit)
        return residue
    }

    fun simpleRecipMulSmallPow10_256(q: Coeff, dw3: Long, dw2: Long, dw1: Long, dw0: Long, pow10: Int): Residue {
        require(pow10 in 1..5)

        val mask48= (1L shl 48) - 1
        val dwA = (dw0 ushr 1) and mask48
        val dwB = ((dw1 shl -49) or (dw0 ushr 49)) and mask48
        val dwC = ((dw2 shl -33) or (dw1 ushr 33)) and mask48
        val dwD = ((dw3 shl -17) or (dw2 ushr 17)) and mask48
        val dwE = (dw3 ushr 1)

        val denom = POW10[pow10] ushr 1
        val M = POW10[(SMALL_SIMPLE_RECIP_POW10sDIV2_OFFSET + pow10) and 0xFF]

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

        val remainder = (rA shl 1) or (dw0 and 1)
        val residue = Residue.residueFromRemainderPow10(remainder, pow10)

        val q3 = qE
        val q2 = (qD shl 16) or (qC ushr 32)
        val q1 = (qC shl 32) or (qB ushr 16)
        val q0 = (qB shl 48) or qA
        q.coeffSet256(q3, q2, q1, q0)
        return residue
    }

    fun simpleRecipMulSmallPow10_208(q: Coeff, dw3: Long, dw2: Long, dw1: Long, dw0: Long, pow10: Int): Residue {
        require(pow10 in 1..5)

        val mask48= (1L shl 48) - 1
        val dwA = (dw0 ushr 1) and mask48
        val dwB = ((dw1 shl -49) or (dw0 ushr 49)) and mask48
        val dwC = ((dw2 shl -33) or (dw1 ushr 33)) and mask48
        val dwE = (dw3 shl -17) or (dw2 ushr 17)

        val denom = POW10[pow10] ushr 1
        val M = POW10[(SMALL_SIMPLE_RECIP_POW10sDIV2_OFFSET + pow10) and 0xFF]

        val qEhat = unsignedMultiplyHigh(dwE, M)
        val rEhat = dwE - (qEhat * denom)
        val adjustE = rEhat >= denom
        val qE = qEhat + if (adjustE) 1L else 0L
        val rE = rEhat - if (adjustE) denom else 0L

        val ppC = (rE shl 48) or dwC
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

        val remainder = (rA shl 1) or (dw0 and 1)
        val residue = Residue.residueFromRemainderPow10(remainder, pow10)

        val q3 = (qE ushr 48)
        val q2 = (qE shl 16) or (qC ushr 32)
        val q1 = (qC shl 32) or (qB ushr 16)
        val q0 = (qB shl 48) or qA
        q.coeffSet256(q3, q2, q1, q0)
        return residue
    }

    fun simpleRecipMulSmallPow10_160(q: Coeff, dw2: Long, dw1: Long, dw0: Long, pow10: Int): Residue {
        require(pow10 in 1..5)

        val mask48= (1L shl 48) - 1
        val dwA = (dw0 ushr 1) and mask48
        val dwB = ((dw1 shl -49) or (dw0 ushr 49)) and mask48
        val dwE = ((dw2 shl -33) or (dw1 ushr 33))

        val denom = POW10[pow10] ushr 1
        val M = POW10[(SMALL_SIMPLE_RECIP_POW10sDIV2_OFFSET + pow10) and 0xFF]

        val qEhat = unsignedMultiplyHigh(dwE, M)
        val rEhat = dwE - (qEhat * denom)
        val adjustE = rEhat >= denom
        val qE = qEhat + if (adjustE) 1L else 0L
        val rE = rEhat - if (adjustE) denom else 0L

        val ppB = (rE shl 48) or dwB
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

        val remainder = (rA shl 1) or (dw0 and 1)
        val residue = Residue.residueFromRemainderPow10(remainder, pow10)

        val q2 = (qE ushr 32)
        val q1 = (qE shl 32) or (qB ushr 16)
        val q0 = (qB shl 48) or qA
        q.coeffSet192(q2, q1, q0)
        return residue
    }

    fun simpleRecipMulSmallPow10_112(q: Coeff, dw1: Long, dw0: Long, pow10: Int): Residue {
        require(pow10 in 1..5)

        val mask48= (1L shl 48) - 1
        val dwA = (dw0 ushr 1) and mask48
        val dwE = ((dw1 shl -49) or (dw0 ushr 49))

        val denom = POW10[pow10] ushr 1
        val M = POW10[(SMALL_SIMPLE_RECIP_POW10sDIV2_OFFSET + pow10) and 0xFF]

        val qEhat = unsignedMultiplyHigh(dwE, M)
        val rEhat = dwE - (qEhat * denom)
        val adjustE = rEhat >= denom
        val qE = qEhat + if (adjustE) 1L else 0L
        val rE = rEhat - if (adjustE) denom else 0L

        val ppA = (rE shl 48) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, M)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = (rA shl 1) or (dw0 and 1)
        val residue = Residue.residueFromRemainderPow10(remainder, pow10)

        val q1 = (qE ushr 16)
        val q0 = (qE shl 48) or qA
        q.coeffSet128(q1, q0)
        return residue
    }

    fun simpleRecipMulSmallPow10_64(q: Coeff, dw0: Long, pow10: Int): Residue {
        require(pow10 in 1..5)

        val dwE = (dw0 ushr 1)

        val denom = POW10[pow10] ushr 1
        val M = POW10[(SMALL_SIMPLE_RECIP_POW10sDIV2_OFFSET + pow10) and 0xFF]

        val qEhat = unsignedMultiplyHigh(dwE, M)
        val rEhat = dwE - (qEhat * denom)
        val adjustE = rEhat >= denom
        val qE = qEhat + if (adjustE) 1L else 0L
        val rE = rEhat - if (adjustE) denom else 0L

        val remainder = (rE shl 1) or (dw0 and 1)
        val residue = Residue.residueFromRemainderPow10(remainder, pow10)

        val q0 = qE
        q.coeffSet64(q0)
        return residue
    }

}
