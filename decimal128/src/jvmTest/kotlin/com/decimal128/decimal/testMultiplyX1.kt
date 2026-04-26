package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

import java.util.Random

class testMultiplyX1 {

    val verbose = false

    fun multiplyX1(
        product:IntArray, productOff:Int,
        multiplicand:IntArray, multiplicandOff:Int, multiplicandLen:Int,
        multiplier:Int) {
        val uMultiplier = multiplier.toLong() and 0xFFFFFFFFL // Treat as unsigned

        var carry = 0L
        for (i in 0..<multiplicandLen) {
            val uMultiplicand = multiplicand[multiplicandOff + i].toLong() and 0xFFFFFFFFL
            val partialProduct = uMultiplicand * uMultiplier + carry
            product[productOff + i] = partialProduct.toInt()
            carry = partialProduct ushr 32
        }

        product[productOff + multiplicandLen] = carry.toInt() // Store any remaining carry
    }

    fun biFromArray(words:IntArray) = biFromArray(words, 0, words.size)

    fun biFromArray(words: IntArray, off: Int, len: Int) : BigInteger {
        var bi = BigInteger.ZERO
        for (i in 0..<len) {
            val biT = BigInteger((words[off + i].toLong() and 0xFFFFFFFFL).toString()).shiftLeft(i * 32)
            bi = bi.or(biT)
        }
        return bi
    }

    data class TC(val multiplicand: IntArray, val multiplier: Int)

    val tcs = arrayOf(
        TC(intArrayOf(0, 1), 1),
        TC(intArrayOf(1), 2),
        TC(intArrayOf(2), 3),

                TC(intArrayOf(1, 2, 3), 4),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            testCase(tc)
    }

    fun testCase(tc: TC) {
        val product = IntArray(tc.multiplicand.size+1)
        multiplyX1(product, 0, tc.multiplicand, 0, tc.multiplicand.size, tc.multiplier)
        val biMultiplicand = biFromArray(tc.multiplicand)
        val biMultiplier = BigInteger(tc.multiplier.toULong().toString())
        val biProduct = biFromArray(product)
        if (verbose)
            println("product:$biProduct multiplicand:$biMultiplicand multiplier ${tc.multiplier}")
        val expected = biMultiplicand.multiply(biMultiplier)
        if (verbose)
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
        val multiplier = random.nextInt()
        val multiplicandLen = (multiplicandBitLength + 31) / 32
        val multiplicandOff = random.nextInt(3)
        val multiplicand = IntArray(multiplicandOff + multiplicandLen)
        for (i in 0..<multiplicandLen)
            multiplicand[multiplicandOff + i] = biMultiplicand.shiftRight(i * 32).toInt()

        val prodOff = random.nextInt(3)
        val prod = IntArray(prodOff + multiplicandLen + 1)

        multiplyX1(prod, prodOff, multiplicand, multiplicandOff, multiplicandLen, multiplier)

        val biProd = biFromArray(prod, prodOff, multiplicandLen + 1)

        val expected = biMultiplicand.multiply(BigInteger((multiplier.toLong() and 0xFFFFFFFFL).toString()))
        if (verbose)
            println("expected:$expected biProd:$biProd")
        assert(expected.equals(biProd))
    }
}