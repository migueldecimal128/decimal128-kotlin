package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger

import java.util.Random

class testMultiplyX4u64 {

    val verbose = false

    fun multiplyX3_oldNames(
                   product: LongArray, productOff: Int,
                   multiplicand: LongArray, multiplicandOff: Int, multiplicandLen: Int,
                   multiplierDw2: Long, multiplierDw1: Long, multiplierDw0: Long) {

        var dw2dw1back3 = 0L
        var dw1dw1back2 = 0L
        var (dw2dw1back2, dw2dw0back2) = 0L to 0L
        var dw0dw1back1 = 0L
        var (dw1dw1back1, dw1dw0back1) = 0L to 0L
        var (dw2dw1back1, dw2dw0back1) = 0L to 0L
        var carryIn = 0L


        var i = 0
        while (i < multiplicandLen + 3) {
            val m = if (i < multiplicandLen) multiplicand[multiplicandOff + i] else 0L

            val (dw0dw1, dw0dw0) = multu64u64u128(m, multiplierDw0)
            val (dw1dw1, dw1dw0) = multu64u64u128(m, multiplierDw1)
            val (dw2dw1, dw2dw0) = multu64u64u128(m, multiplierDw2)

            val (carry, currentDigit) = sumU64(dw2dw1back3, dw1dw1back2, dw2dw0back2, dw0dw1back1, dw1dw0back1, dw0dw0, carryIn)
            product[productOff + i] = currentDigit

            dw2dw1back3 = dw2dw1back2
            dw2dw1back2 = dw2dw1back1
            dw2dw0back2 = dw2dw0back1
            dw2dw1back1 = dw2dw1
            dw2dw0back1 = dw2dw0

            dw1dw1back2 = dw1dw1back1
            dw1dw1back1 = dw1dw1
            dw1dw0back1 = dw1dw0

            dw0dw1back1 = dw0dw1
            carryIn = carry

            ++i
        }
    }

    fun multiplyX4(
        product: LongArray, productOff: Int,
        multiplicand: LongArray, multiplicandOff: Int, multiplicandLen: Int,
        m3: Long, m2: Long, m1: Long, m0: Long) {

        var pp31_4 = 0L; var pp31_3 = 0L; var pp31_2 = 0L; var pp31_1 = 0L
        var pp30_3 = 0L; var pp30_2 = 0L; var pp30_1 = 0L

        var pp21_3 = 0L; var pp21_2 = 0L; var pp21_1 = 0L
        var pp20_2 = 0L; var pp20_1 = 0L;

        var pp11_2 = 0L; var pp11_1 = 0L;
        var pp10_1 = 0L

        var pp01_1 = 0L
        var carryIn = 0L


        var i = 0
        while (i < multiplicandLen + 4) {
            val m = if (i < multiplicandLen) multiplicand[multiplicandOff + i] else 0L

            val (pp01_0, pp00_0) = multu64u64u128(m, m0)
            val (pp11_0, pp10_0) = multu64u64u128(m, m1)
            val (pp21_0, pp20_0) = multu64u64u128(m, m2)
            val (pp31_0, pp30_0) = multu64u64u128(m, m3)

            assert (pp31_4 == 0L)
            assert (pp30_3 == 0L)
            val (carry, currentDigit) = sumU64(pp31_4, pp30_3, pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, pp00_0, carryIn)

            product[productOff + i] = currentDigit

            pp21_3 = pp21_2
            pp21_2 = pp21_1
            pp21_1 = pp21_0

            pp20_2 = pp20_1
            pp20_1 = pp20_0

            pp11_2 = pp11_1
            pp11_1 = pp11_0

            pp10_1 = pp10_0

            pp01_1 = pp01_0

            carryIn = carry

            ++i
        }
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

    class TC(val multiplicand: LongArray, val multiplierDw3: Long, val multiplierDw2: Long, val multiplierDw1: Long, val multiplierDw0: Long)

    val tcs = arrayOf(
        TC(longArrayOf(-1, -1, -1, -1), 0L, 1, 0, 1),
        //TC(longArrayOf(-1, 1L shl 63), 0L, -1, -2, -3),

        TC(longArrayOf(0, -2), 0L, 0L, -4, -5),
        TC(longArrayOf(-1, -1), 0L, 0L, -1, 0),
        TC(longArrayOf(-1, -1), 0L, 0L, 0, -1),
        TC(longArrayOf(-1, -1), 0L, 0L, -1, 0),
        TC(longArrayOf(-1, -1), 0L, 0L, 0, 1),
        TC(longArrayOf(-1, -1), 0L, 0L, 1, 0),
        TC(longArrayOf(1), 0, 0L, 0, 1),
        TC(longArrayOf(1), 0,0L, 1, 0),
        TC(longArrayOf(-1), 0, 0L, 0, 1),
        TC(longArrayOf(-1), 0, 0L, 1, 0),
        TC(longArrayOf(1), 0, 0L, 0, -1),
        TC(longArrayOf(1), 0L, 0,-1, 0),
        TC(longArrayOf(-1), 0, 0L, 0, -1),
        TC(longArrayOf(-1), 0, 0L, -1, 0),
        TC(longArrayOf(1), 0L, 0L, 0, 1),
        TC(longArrayOf(1), 0L, 0L, 1, 0),
        TC(longArrayOf(1), 0L, 0L, 0, 1),
        TC(arrayFrom("2864237486"),  0L, 0L, dw1From("7329510386"), dw0From("7329510386")),
        TC(arrayFrom("3572443395"), 0L, 0L, dw1From("6245952187"), dw0From("6245952187")),
        TC(arrayFrom("3273291708"), 0L, 0L, dw1From("7927391716"), dw0From("7927391716")),
        TC(longArrayOf(1), 0L, 0L, 1, 0),
        TC(longArrayOf(2), 0L, 0L, 3, 4),
        TC(longArrayOf(0, 1), 0L, 0L, 1, 0),
        TC(longArrayOf(1, 2, 3), 0L, 0L, 4, 5),
        TC(longArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), 0L, 0L, 2, (1 shl 31)),
        TC(longArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), 0L, 0L, 1, (1 shl 31)),

        TC(longArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), 0L, 0L, (1 shl 31), (1 shl 31)),

        TC(longArrayOf(-1, -1), 0L, -1, 0, 0),

        TC(longArrayOf(-1, -1), 0L, -1, 0, 0),
        TC(longArrayOf(-1, -1), 0L, 1, 0, 0),

        TC(longArrayOf(-1), 0L, -1, 0, 0),

        TC(longArrayOf(1), 0L, 1, 0, 0),
        TC(longArrayOf(-1), 0L, 1, 0, 0),
        TC(longArrayOf(1), 0L, -1, 0, 0),
        TC(longArrayOf(-1), 0L, -1, 0, 0),

        TC(longArrayOf(1), 0L, 1L, 0, 0),
        TC(longArrayOf(1), 0L, 4L, 2, 0),
        TC(longArrayOf((1 shl 63)), 0L, 1L, 0, 1 shl 63),
        TC(longArrayOf(1), 0L, 1L shl 63, 0, 0),
        TC(longArrayOf(-1), 0L, -1L, -1L, -1L),

        TC(longArrayOf(-1, -1, -1, -1), 0L, -1, -1, -1),

    )

    @Test
    fun testSumU64() {
        val (carry9, sum9) = sumU64(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L)
        assert (carry9 == 0L)
        assert (sum9 == 9L)
        val (carryFF, sumFF) = sumU64(-1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L)
        assertEquals(8L, carryFF)
        assertEquals(-9, sumFF)
    }

    @Test
    fun testCases() {
        for (tc in tcs)
            testCase(tc)
    }

    fun testCase(tc: TC) {
        val biMultiplicand = biFromArray(tc.multiplicand)
        val biMultiplier = biFrom(tc.multiplierDw2, tc.multiplierDw1, tc.multiplierDw0)
        val expected = biMultiplicand.multiply(biMultiplier)
        val expectedAsArray = arrayFrom(expected)
        val product = LongArray(tc.multiplicand.size + 4)
        multiplyX4(product, 0, tc.multiplicand, 0, tc.multiplicand.size, tc.multiplierDw3, tc.multiplierDw2, tc.multiplierDw1, tc.multiplierDw0)
        val biProduct = biFromArray(product)
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
        val multiplicandBitLength = random.nextInt(1, 1000)
        val biMultiplicand = BigInteger(multiplicandBitLength, random)
        val multiplicandLen = (multiplicandBitLength + 63) / 64
        val multiplicandOff = random.nextInt(3)
        val multiplicand = LongArray(multiplicandOff + multiplicandLen)
        for (i in 0..<multiplicandLen)
            multiplicand[multiplicandOff + i] = biMultiplicand.shiftRight(i * 64).toLong()

        val multiplierBitLength = random.nextInt(1, 193)
        val biMultiplier = BigInteger(multiplierBitLength, random)
        val multiplierDw3 = biMultiplier.shiftRight(192).toLong()
        val multiplierDw2 = biMultiplier.shiftRight(128).toLong()
        val multiplierDw1 = biMultiplier.shiftRight(64).toLong()
        val multiplierDw0 = biMultiplier.toLong()

        val prodOff = random.nextInt(3)
        val prod = LongArray(prodOff + multiplicandLen + 4)
        val expected = biMultiplicand.multiply(biMultiplier)
        val expectedAsArray = arrayFrom(expected)

        multiplyX4(prod, prodOff, multiplicand, multiplicandOff, multiplicandLen,
            multiplierDw3, multiplierDw2, multiplierDw1, multiplierDw0)

        val biProd = biFromArray(prod, prodOff, multiplicandLen + 4)

        if (! expected.equals(biProd))
            println("expected:$expected observed:$biProd multiplicand:$biMultiplicand multiplier $biMultiplier")
        assert(expected.equals(biProd))
    }
}