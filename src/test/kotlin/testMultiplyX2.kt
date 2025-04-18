package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger

import java.util.Random

class testMultiplyX2 {

    fun multiplyX2(
        product:IntArray, productOff:Int,
        multiplicand:IntArray, multiplicandOff:Int, multiplicandLen:Int,
        multiplierW1:Int, multiplierW0:Int) {

        val mHi = multiplierW1.toLong() and 0xFFFFFFFFL // multiplier hi word 1
        val mLo = multiplierW0.toLong() and 0xFFFFFFFFL // multiplier lo word 0

        var uMultiplicand = multiplicand[multiplicandOff].toLong() and 0xFFFFFFFFL
        var partialLo = mLo * uMultiplicand
        var currentDigit = partialLo // and 0xFFFFFFFFL
        product[productOff] = currentDigit.toInt()
        var partialHi = mHi * uMultiplicand // + (currentDigit ushr 32)
        var partialHiPrePrev = 0L
        var partialHiPrev = partialHi
        var partialLoPrev = partialLo
        var i = 1
        while (i < multiplicandLen) {
            uMultiplicand = multiplicand[multiplicandOff + i].toLong() and 0xFFFFFFFFL

            partialLo = mLo * uMultiplicand
            currentDigit = (partialHiPrePrev ushr 32) + (partialHiPrev and 0xFFFFFFFFL) + (partialLoPrev ushr 32) + (partialLo and 0xFFFFFFFFL)
            product[productOff + i] = currentDigit.toInt()
            partialHi = mHi * uMultiplicand + (currentDigit ushr 32)
            partialHiPrePrev = partialHiPrev
            partialHiPrev = partialHi
            partialLoPrev = partialLo
            ++i
        }

        val final2Digits = (partialHiPrePrev ushr 32) + (partialHiPrev /*and 0xFFFFFFFFL*/) + (partialLoPrev ushr 32)
        product[productOff + i] = final2Digits.toInt()
        product[productOff + i + 1] = (final2Digits ushr 32).toInt()
    }

    fun arrayFrom(str:String) = arrayFrom(BigInteger(str))

    fun arrayFrom(bi: BigInteger) : IntArray {
        if (bi.equals(BigInteger.ZERO))
            return intArrayOf(0)
        var biT = bi
        val arrayList = ArrayList<Int>()
        do {
            arrayList.add(biT.toInt())
            biT = biT.shiftRight(32)
        } while (! biT.equals(BigInteger.ZERO))
        return arrayList.toIntArray()
    }

    fun biFromArray(words: IntArray) = biFromArray(words, 0, words.size)

    fun biFromArray(words: IntArray, off: Int, len: Int) : BigInteger {
        var bi = BigInteger.ZERO
        for (i in 0..<len) {
            val biT = biFrom(words[off + i]).shiftLeft(i * 32)
            bi = bi.or(biT)
        }
        return bi
    }

    fun w1From(str: String) = BigInteger(str).shiftRight(32).toInt()
    fun w0From(str: String) = BigInteger(str).toInt()

    fun biFrom(w: Int) = BigInteger((w.toLong() and 0xFFFFFFFFL).toString())

    fun biFrom(w1: Int, w0: Int) = biFrom(w1).shiftLeft(32).or(biFrom(w0))

    data class TC(val multiplicand: IntArray, val multiplierW1: Int, val multiplierW0: Int)

    val tcs = arrayOf(
        TC(arrayFrom("2864237486"), w1From("7329510386"), w0From("7329510386")),
        TC(intArrayOf(-1, -1, -1, -1), -1, -1),
        TC(intArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), 2, (1 shl 31)),

        TC(intArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), 1, (1 shl 31)),
        TC(arrayFrom("3572443395"), w1From("6245952187"), w0From("6245952187")),
        TC(arrayFrom("3273291708"), w1From("7927391716"), w0From("7927391716")),

        TC(intArrayOf(1), 1, 0),
        TC(intArrayOf(2), 3, 4),
        TC(intArrayOf(0, 1), 1, 0),
        TC(intArrayOf(1, 2, 3), 4, 5),
        TC(intArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), (1 shl 31), (1 shl 31)),
        TC(arrayFrom(BigInteger("5035237707799603283")), BigInteger("63885433061416").shiftRight(256).toInt(), BigInteger("63885433061416").toInt()),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            testCase(tc)
    }

    fun testCase(tc: TC) {
        val product = IntArray(tc.multiplicand.size + 2)
        multiplyX2(product, 0, tc.multiplicand, 0, tc.multiplicand.size, tc.multiplierW1, tc.multiplierW0)
        val biMultiplicand = biFromArray(tc.multiplicand)
        val biMultiplier = biFrom(tc.multiplierW1, tc.multiplierW0)
        val biProduct = biFromArray(product)
        val expected = biMultiplicand.multiply(biMultiplier)
        val expectedAsArray = arrayFrom(expected)
        if (! expected.equals(biProduct))
            println("expected:$expected observed:$biProduct multiplicand:$biMultiplicand multiplier $biMultiplier")
        assert(expected.equals(biProduct))
    }

    @Test
    fun testRandoms() {
        for (i in 0..<100000)
            testRandom1()
    }

    val random = Random()

    fun testRandom1() {
        val multiplicandBitLength = random.nextInt(1, 257)
        val biMultiplicand = BigInteger(multiplicandBitLength, random)
        val multiplicandLen = (multiplicandBitLength + 31) / 32
        val multiplicandOff = random.nextInt(3)
        val multiplicand = IntArray(multiplicandOff + multiplicandLen)
        for (i in 0..<multiplicandLen)
            multiplicand[multiplicandOff + i] = biMultiplicand.shiftRight(i * 32).toInt()

        val multiplierBitLength = random.nextInt(33, 65)
        val biMultiplier = BigInteger(multiplierBitLength, random)
        val multiplierW1 = biMultiplier.shiftRight(32).toInt()
        val multiplierW0 = biMultiplier.toInt()

        val prodOff = random.nextInt(3)
        val prod = IntArray(prodOff + multiplicandLen + 2)

        multiplyX2(prod, prodOff, multiplicand, multiplicandOff, multiplicandLen, multiplierW1, multiplierW0)

        val biProd = biFromArray(prod, prodOff, multiplicandLen + 2)

        val expected = biMultiplicand.multiply(biMultiplier)
        if (! expected.equals(biProd))
            println("expected:$expected observed:$biProd multiplicand:$biMultiplicand multiplier $biMultiplier")
        assert(expected.equals(biProd))
    }
}