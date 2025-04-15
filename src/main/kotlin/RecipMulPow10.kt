package com.decimal128

import java.math.BigInteger

val MIN_DIVIDEND_DIGIT_COUNT = 2
val MAX_DIVIDEND_DIGIT_COUNT = 78 // exclusive
val MIN_DIVISOR_POW10 = 1
val MAX_DIVISOR_POW10 = MAX_DIVIDEND_DIGIT_COUNT - 34

class RecipMulPow10 {

    companion object {

        val bi0 = BigInteger.ZERO
        val bi1 = BigInteger.ONE
        val bi3 = 3.toBigInteger()
        val bi5 = 5.toBigInteger()
        val bi10 = BigInteger.TEN


        class RecipMulParams5(
            val dividendDigitCount: Int, val divisorPow10: Int,
            val mul: BigInteger, val shift: Int
        ) {
            val dividend9 = bi10.pow(dividendDigitCount).subtract(bi1)
            // for rounding, we need to leave one more bit in the dividend before dividing
            val dividendShiftWithRoundingBit = divisorPow10-1
            val maxProd = dividend9.shiftRight(dividendShiftWithRoundingBit).multiply(mul)
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

            override fun toString(): String {
                return "dividendDigitCount10:$dividendDigitCount divisorPow10:$divisorPow10\n" +
                        "  mulDwordCount:$mulDwordCount mulBitCount:$mulBitCount mulDigitCount:$mulDigitCount\n" +
                        "  dividend9:$dividend9 mul:$mul\n" +
                        "  accDwordCount:$accDwordCount accBitCount:$accBitCount shift:$shift\n" +
                        "  quotDwordCount:$quotDwordCount quotBitCount:$quotBitCount\n" +
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
            if (params.quotBitCount > 0 && shift % 64 != 0) {
                // try rounding up to next 64-bit boundary
                val shift64 = ((shift + 63) / 64) * 64
                val (mul64, shiftT) = generateMulAndShift(biDividend5, biDivisor5, shift64)
                require(shiftT == shift64)
                val params64 = RecipMulParams5(dividendDigitCount, divisorPow10, mul64, shift64)
                require(params.quotBitCount == params64.quotBitCount)
                if ((params64.accDwordCount == params.accDwordCount) && (params64.mulDwordCount == params.mulDwordCount)) {
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


        var initialized = false

        val rowSize = MAX_DIVISOR_POW10 - MIN_DIVISOR_POW10
        val tableSize = (MAX_DIVIDEND_DIGIT_COUNT - MIN_DIVIDEND_DIGIT_COUNT) * rowSize

        val indexesPow5 = IntArray(tableSize)
        var paramsPow5 = LongArray(0)

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
            for (digitCount10 in MIN_DIVIDEND_DIGIT_COUNT..<MAX_DIVIDEND_DIGIT_COUNT) {
                val maxPow10 = Math.min(digitCount10, MAX_DIVISOR_POW10)
                for (pow10 in MIN_DIVISOR_POW10..<maxPow10) {
                    val index = indexOf(digitCount10, pow10)
                    val rmp5 = generateRecipMulParams5(digitCount10, pow10)
                    if (rmp5.quotDwordCount == 0) {
                        indexesPow5[index] = 0
                    } else {
                        val paramsIndex = serializeParams(paramsArrayList, rmp5)
//                    println("digitCount10:$digitCount10 pow10:$pow10 $rmp5")
//                    println()
                        indexesPow5[index] = paramsIndex
                    }
                }
            }
            paramsPow5 = paramsArrayList.toLongArray()
            println("initialized: params.size:${paramsPow5.size}")
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
                    val paramsIndex = indexesPow5[index]
                    if (paramsIndex == 0)
                        continue
                    val descriptor = paramsPow5[paramsIndex].toInt()
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

        fun divModPow10(q: Coefficient, r: Coefficient, d: Coefficient, pow10: Int) {
            throw RuntimeException("not impl")
        }


        fun getMultShift(digitCount: Int, pow10: Int) : Pair<BigInteger, Int> {
            throw RuntimeException("not impl")
        }

    }


}
