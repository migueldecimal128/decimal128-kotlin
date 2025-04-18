package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger

import java.util.Random

class testMultiplyX1u64 {

    fun multu64u64u128(a: Long, b: Long) : Pair<Long, Long> {
        val hiSigned = Math.multiplyHigh(a, b)
        val lo = a * b
        val unsignedCorrection = ((a shr 63) and b) + ((b shr 63) and a)
        val hiUnsigned = hiSigned + unsignedCorrection
        return hiUnsigned to lo
    }

    fun unsignedFMA64(a: Long, b: Long, carryIn: Long) : Pair<Long, Long> {
        val unsignedCorrection = ((a shr 63) and b) + ((b shr 63) and a)
        val loBase = a * b
        val hiSigned = Math.multiplyHigh(a, b)
        val hiBase = hiSigned + unsignedCorrection
        val lo = loBase + carryIn
        val carryWithin = if ((lo xor Long.MIN_VALUE) < (loBase xor Long.MIN_VALUE)) 1L else 0L
        val hi = hiBase + carryWithin
        return hi to lo
    }


    fun multiplyX1u64(
        product: LongArray, productOff: Int,
        multiplicand: LongArray, multiplicandOff: Int, multiplicandLen: Int,
        multiplier: Long) {
        val uMultiplier = multiplier.toLong() and 0xFFFFFFFFL // Treat as unsigned

        var carry = 0L
        for (i in 0..<multiplicandLen) {
            val m = multiplicand[multiplicandOff + i]
            val (partialHi, partialLo) = unsignedFMA64(m, multiplier, carry)
            product[productOff + i] = partialLo
            carry = partialHi
        }

        product[productOff + multiplicandLen] = carry // Store any remaining carry
    }

    fun biFromArray(dwords: LongArray) = biFromArray(dwords, 0, dwords.size)

    fun biFromArray(dwords: LongArray, off: Int, len: Int) : BigInteger {
        var bi = BigInteger.ZERO
        for (i in 0..<len) {
            val biT = BigInteger(dwords[off + i].toULong().toString()).shiftLeft(i * 64)
            bi = bi.or(biT)
        }
        return bi
    }

    fun arrayFrom(str: String) = arrayFrom(BigInteger(str))

    fun arrayFrom(bi: BigInteger) : LongArray {
        if (bi.equals(BigInteger.ZERO))
            return longArrayOf(0)
        var biT = bi
        val arrayList = ArrayList<Long>()
        do {
            arrayList.add(biT.toLong())
            biT = biT.shiftRight(64)
        } while (! biT.equals(BigInteger.ZERO))
        return arrayList.toLongArray()
    }


    data class TC(val multiplicand: LongArray, val multiplier: Long)

    val tcs = arrayOf(
        TC(longArrayOf(-1), 1),
        TC(longArrayOf(0, 1), 1),
        TC(longArrayOf(1), 2),
        TC(longArrayOf(2), 3),

        TC(longArrayOf(1, 2, 3), 4),
        TC(longArrayOf(-1, -1, -1, -1), -1)
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            testCase(tc)
    }

    fun testCase(tc: TC) {
        val product = LongArray(tc.multiplicand.size+1)
        multiplyX1u64(product, 0, tc.multiplicand, 0, tc.multiplicand.size, tc.multiplier)
        val biMultiplicand = biFromArray(tc.multiplicand)
        val biMultiplier = BigInteger(tc.multiplier.toULong().toString())
        val biProduct = biFromArray(product)
        println("product:$biProduct multiplicand:$biMultiplicand multiplier ${tc.multiplier}")
        val expected = biMultiplicand.multiply(biMultiplier)
        println("expected:$expected")
        assert(expected.equals(biProduct))
    }

    @Test
    fun testRandoms() {
        for (i in 0..<10000)
            testRandom1()
    }

    val random = Random()

    fun testRandom1() {
        val multiplicandBitLength = random.nextInt(1, 255)
        val biMultiplicand = BigInteger(multiplicandBitLength, random)
        val multiplicandLen = (multiplicandBitLength + 63) / 64
        val multiplicandOff = random.nextInt(3)
        val multiplicand = LongArray(multiplicandOff + multiplicandLen + (multiplicandOff and 1))
        for (i in 0..<multiplicandLen)
            multiplicand[multiplicandOff + i] = biMultiplicand.shiftRight(i * 64).toLong()

        val multiplierBitLength = random.nextInt(1, 65)
        val biMultiplier = BigInteger(multiplierBitLength, random)
        val multiplierW0 = biMultiplier.toLong()

        val prodOff = random.nextInt(3)
        val prod = LongArray(prodOff + multiplicandLen + 1)

        multiplyX1u64(prod, prodOff, multiplicand, multiplicandOff, multiplicandLen, multiplierW0)

        val biProd = biFromArray(prod, prodOff, multiplicandLen + 1)

        val expected = biMultiplicand.multiply(biMultiplier)
        println("expected:$expected biProd:$biProd")
        assert(expected.equals(biProd))
    }
}