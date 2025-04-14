package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger

import java.util.Random

class testMultiplyX2u64 {

    fun multiplyX2_oldNames2(
        product: LongArray, productOff: Int,
        multiplicand: LongArray, multiplicandOff: Int, multiplicandLen: Int,
        multiplierDw1: Long, multiplierDw0: Long) {

        val uMultiplicand0 = multiplicand[multiplicandOff]
        val (partialLoDw1x, partialLoDw0x) = multu64u64u128(uMultiplicand0, multiplierDw0)
        product[productOff] = partialLoDw0x
        var (partialHiPrevDw1, partialHiPrevDw0) = multu64u64u128(uMultiplicand0, multiplierDw1)
        var partialHiPrePrevDw1 = 0L
        var partialLoPrevDw1 = partialLoDw1x

        var i = 1
        while (i < multiplicandLen) {
            val uMultiplicand = multiplicand[multiplicandOff + i]

            val (partialLoDw1, partialLoDw0) = multu64u64u128(uMultiplicand, multiplierDw0)

            val (carry, currentDigit) = sumU64(partialHiPrePrevDw1, partialHiPrevDw0, partialLoPrevDw1, partialLoDw0)
            product[productOff + i] = currentDigit
            val (partialHiDw1, partialHiDw0) = unsignedFMA64(uMultiplicand, multiplierDw1, carry)

            partialHiPrePrevDw1 = partialHiPrevDw1

            partialHiPrevDw1 = partialHiDw1
            partialHiPrevDw0 = partialHiDw0

            partialLoPrevDw1 = partialLoDw1

            ++i
        }

        val (finalDw1, finalDw0) = sumU64(partialHiPrePrevDw1, partialHiPrevDw0, partialLoPrevDw1)
        product[productOff + i] = finalDw0
        product[productOff + i + 1] = finalDw1 + partialHiPrevDw1
    }

    fun multiplyX2_changeNames(
        product: LongArray, productOff: Int,
        multiplicand: LongArray, multiplicandOff: Int, multiplicandLen: Int,
        multiplierDw1: Long, multiplierDw0: Long) {

        /*
        val m0 = multiplicand[multiplicandOff]
        val (dw0dw1, dw0dw0) = multu64u64u128(m0, multiplierDw0)
        product[productOff] = dw0dw0
        var (dw1dw1back1, dw1dw0back1) = multu64u64u128(m0, multiplierDw1)
         */
        var dw1dw1back2 = 0L
        var (dw1dw1back1, dw1dw0back1) = 0L to 0L
        var dw0dw1back1 = 0L

        var i = 0
        while (i < multiplicandLen + 2) {
            val m = if (i < multiplicandLen) multiplicand[multiplicandOff + i] else 0L

            val (dw0dw1, dw0dw0) = multu64u64u128(m, multiplierDw0)

            val (carry, currentDigit) = sumU64(dw1dw1back2, dw1dw0back1, dw0dw1back1, dw0dw0)
            product[productOff + i] = currentDigit
            val (dw1dw1, dw1dw0) = unsignedFMA64(m, multiplierDw1, carry)

            dw1dw1back2 = dw1dw1back1

            dw1dw1back1 = dw1dw1
            dw1dw0back1 = dw1dw0

            dw0dw1back1 = dw0dw1

            ++i
        }

        //val (finalDw1, finalDw0) = sumU64(dw1dw1back2, dw1dw0back1, dw0dw1back1)
        //product[productOff + i] = finalDw0
        //product[productOff + i + 1] = finalDw1 + dw1dw1back1
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

    data class TC(val multiplicand: LongArray, val multiplierW1: Long, val multiplierW0: Long)

    val tcs = arrayOf(
        TC(longArrayOf(-1), 2, 0),
        TC(longArrayOf((1 shl 63)), 0, 1 shl 63),
        TC(longArrayOf(1), 0, 1),
        TC(longArrayOf(1), 1, 0),
        TC(arrayFrom("2864237486"), dw1From("7329510386"), dw0From("7329510386")),
        TC(longArrayOf(-1, -1, -1, -1), -1, -1),
        TC(longArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), 2, (1 shl 31)),

        TC(longArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), 1, (1 shl 31)),
        TC(arrayFrom("3572443395"), dw1From("6245952187"), dw0From("6245952187")),
        TC(arrayFrom("3273291708"), dw1From("7927391716"), dw0From("7927391716")),

        TC(longArrayOf(1), 1, 0),
        TC(longArrayOf(2), 3, 4),
        TC(longArrayOf(0, 1), 1, 0),
        TC(longArrayOf(1, 2, 3), 4, 5),
        TC(longArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), (1 shl 31), (1 shl 31)),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            testCase(tc)
    }

    fun testCase(tc: TC) {
        val product = LongArray(tc.multiplicand.size + 2)
        multiplyX2_changeNames(product, 0, tc.multiplicand, 0, tc.multiplicand.size, tc.multiplierW1, tc.multiplierW0)
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
        val multiplicand = LongArray(multiplicandOff + multiplicandLen)
        for (i in 0..<multiplicandLen)
            multiplicand[multiplicandOff + i] = biMultiplicand.shiftRight(i * 64).toLong()

        val multiplierBitLength = random.nextInt(33, 129)
        val biMultiplier = BigInteger(multiplierBitLength, random)
        val multiplierDw1 = biMultiplier.shiftRight(64).toLong()
        val multiplierDw0 = biMultiplier.toLong()

        val prodOff = random.nextInt(3)
        val prod = LongArray(prodOff + multiplicandLen + 2)

        multiplyX2_changeNames(prod, prodOff, multiplicand, multiplicandOff, multiplicandLen, multiplierDw1, multiplierDw0)

        val biProd = biFromArray(prod, prodOff, multiplicandLen + 2)

        val expected = biMultiplicand.multiply(biMultiplier)
        if (! expected.equals(biProd))
            println("expected:$expected observed:$biProd multiplicand:$biMultiplicand multiplier $biMultiplier")
        assert(expected.equals(biProd))
    }
}