package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger

import java.util.Random

class testMultiplyX3u32_noFMA {

    fun sumU32(wA: Int, wB: Int) : Pair<Int, Int> {
        val sum = wA + wB
        val carry = if ((sum xor Int.MIN_VALUE) < (wA xor Int.MIN_VALUE)) 1 else 0

        val validateULong = wA.toULong() + wB.toULong()
        require (((carry.toULong() shl 32) or sum.toULong()) == validateULong)

        return sum to carry
    }

    fun sumU32(wA: Int, wB: Int, wC: Int) : Pair<Int, Int> {
        val sumAB = wA + wB
        val carryAB = if ((sumAB xor Int.MIN_VALUE) < (wA xor Int.MIN_VALUE)) 1 else 0

        val sum = sumAB + wC
        val carry = carryAB + if ((sum xor Int.MIN_VALUE) < (sumAB xor Int.MIN_VALUE)) 1 else 0

        val validateULong = wA.toULong() + wB.toULong() + wC.toULong()
        require (((carry.toULong() shl 32) or sum.toULong()) == validateULong)

        return carry to sum
    }

    fun sumU32(wA: Int, wB: Int, wC: Int, wD: Int) : Pair<Int, Int> {
        val sumAB = wA + wB
        val carryAB = if ((sumAB xor Int.MIN_VALUE) < (wA xor Int.MIN_VALUE)) 1 else 0

        val sumCD = wC + wD
        val carryCD = if ((sumCD xor Int.MIN_VALUE) < (wC xor Int.MIN_VALUE)) 1 else 0

        val sum = sumAB + sumCD
        val carry = carryAB + carryCD + if ((sum xor Int.MIN_VALUE) < (sumAB xor Int.MIN_VALUE)) 1 else 0

        val validateULong = wA.toULong() + wB.toULong() + wC.toULong() + wD.toULong()
        require (((carry.toULong() shl 32) or sum.toULong()) == validateULong)

        return carry to sum
    }

    fun sumU32(wA: Int, wB: Int, wC: Int, wD: Int, wE: Int) : Pair<Int, Int> {
        val sumAB = wA + wB
        val carryAB = if ((sumAB xor Int.MIN_VALUE) < (wA xor Int.MIN_VALUE)) 1 else 0

        val sumCD = wC + wD
        val carryCD = if ((sumCD xor Int.MIN_VALUE) < (wC xor Int.MIN_VALUE)) 1 else 0

        val sumABCD = sumAB + sumCD
        val carryABCD = carryAB + carryCD + if ((sumABCD xor Int.MIN_VALUE) < (sumAB xor Int.MIN_VALUE)) 1 else 0

        val sum = sumABCD + wE
        val carry = carryABCD + if ((sum xor Int.MIN_VALUE) < (sumABCD xor Int.MIN_VALUE)) 1 else 0

        val validateULong = wA.toULong() + wB.toULong() + wC.toULong() + wD.toULong() + wE.toULong()
        require (((carry.toULong() shl 32) or sum.toULong()) == validateULong)

        return carry to sum
    }

    fun sumU32(wA: Int, wB: Int, wC: Int, wD: Int, wE: Int, wF: Int) : Pair<Int, Int> {
        val sumAB = wA + wB
        val carryAB = if ((sumAB xor Int.MIN_VALUE) < (wA xor Int.MIN_VALUE)) 1 else 0

        val sumCD = wC + wD
        val carryCD = if ((sumCD xor Int.MIN_VALUE) < (wC xor Int.MIN_VALUE)) 1 else 0

        val sumABCD = sumAB + sumCD
        val carryABCD = carryAB + carryCD + if ((sumABCD xor Int.MIN_VALUE) < (sumAB xor Int.MIN_VALUE)) 1 else 0

        val sumEF = wE + wF
        val carryEF = if ((sumEF xor Int.MIN_VALUE) < (wE xor Int.MIN_VALUE)) 1 else 0

        val sum = sumABCD + sumEF
        val carry = carryABCD + carryEF + if ((sum xor Int.MIN_VALUE) < (sumABCD xor Int.MIN_VALUE)) 1 else 0

        val validateULong = wA.toULong() + wB.toULong() + wC.toULong() + wD.toULong() + wE.toULong() + wF.toULong()
        require (((carry.toULong() shl 32) or sum.toULong()) == validateULong)

        return carry to sum
    }

    fun asLong(a: Int) = (a.toLong() and 0xFFFFFFFFL)

    fun sumU32(wA: Int, wB: Int, wC: Int, wD: Int, wE: Int, wF: Int, wG: Int) : Pair<Int, Int> {
        val sumAB = wA + wB
        val carryAB = if ((sumAB xor Int.MIN_VALUE) < (wA xor Int.MIN_VALUE)) 1 else 0

        val sumCD = wC + wD
        val carryCD = if ((sumCD xor Int.MIN_VALUE) < (wC xor Int.MIN_VALUE)) 1 else 0

        val sumABCD = sumAB + sumCD
        val carryABCD = carryAB + carryCD + if ((sumABCD xor Int.MIN_VALUE) < (sumAB xor Int.MIN_VALUE)) 1 else 0

        val sumEF = wE + wF
        val carryEF = if ((sumEF xor Int.MIN_VALUE) < (wE xor Int.MIN_VALUE)) 1 else 0

        val sumEFG = sumEF + wG
        val carryEFG = carryEF + if ((sumEFG xor Int.MIN_VALUE) < (wG xor Int.MIN_VALUE)) 1 else 0

        val sum = sumABCD + sumEFG
        val carry = carryABCD + carryEFG + if ((sum xor Int.MIN_VALUE) < (sumABCD xor Int.MIN_VALUE)) 1 else 0

        val validateLong = asLong(wA) + asLong(wB) + asLong(wC) + asLong(wD) + asLong(wE) + asLong(wF) + asLong(wG)
        require (((asLong(carry) shl 32) or asLong(sum)) == validateLong)

        return carry to sum
    }

    fun multu32u32u64(a: Int, b: Int): Pair<Int, Int> {
        val productSigned = a.toLong() * b.toLong()
        val productUnsigned = (a.toLong() and 0xFFFFFFFFL) * (b.toLong() and 0xFFFFFFFFL)
        val hiSigned = (productSigned shr 32).toInt()
        val lo = a * b
        val unsignedCorrection = ((a shr 31) and b) + ((b shr 31) and a)
        val hiUnsigned = hiSigned + unsignedCorrection
        val productViaSigned = (hiUnsigned.toLong() shl 32) or (lo.toLong() and 0xFFFFFFFFL)
        require (productViaSigned == productUnsigned)
        return hiUnsigned to lo
    }

    fun unsignedFMA32(a: Int, b: Int, carryIn: Int): Pair<Int, Int> {
        val fmaUnsigned = ((a.toLong() and 0xFFFFFFFFL) * (b.toLong() and 0xFFFFFFFFL)) + carryIn.toLong()

        val unsignedCorrection = ((a shr 31) and b) + ((b shr 31) and a)
        val loBase = a * b
        //val hiSigned = Math.multiplyHigh(a, b)
        val hiSigned = ((a.toLong() * b.toLong()) shr 32).toInt()
        val hiBase = hiSigned + unsignedCorrection
        val lo = loBase + carryIn
        val carryWithin = if ((lo xor Int.MIN_VALUE) < (loBase xor Int.MIN_VALUE)) 1 else 0
        val hi = hiBase + carryWithin
        require(((hi.toLong() and 0xFFFFFFFFL) shl 32) or (lo.toLong() and 0xFFFFFFFFL) == fmaUnsigned)
        return hi to lo
    }



    fun multiplyX3_worksFor2(
        product: IntArray, productOff: Int,
        multiplicand: IntArray, multiplicandOff: Int, multiplicandLen: Int,
        multiplierDw2: Int, multiplierDw1: Int, multiplierDw0: Int) {

        var dw1dw1back2 = 0
        var dw0dw1back1 = 0
        var (dw1dw1back1, dw1dw0back1) = 0 to 0

        var i = 0
        while (i < multiplicandLen + 3) {
            val m = if (i < multiplicandLen) multiplicand[multiplicandOff + i] else 0

            val (dw0dw1, dw0dw0) = multu32u32u64(m, multiplierDw0)

            val (carry, currentDigit) = sumU32(dw1dw1back2, dw0dw1back1, dw1dw0back1, dw0dw0)
            product[productOff + i] = currentDigit
            val (dw1dw1, dw1dw0) = unsignedFMA32(m, multiplierDw1, carry)

            dw1dw1back2 = dw1dw1back1
            dw0dw1back1 = dw0dw1
            dw1dw1back1 = dw1dw1
            dw1dw0back1 = dw1dw0


            ++i
        }
    }


    fun multiplyX3(
        expectedAsArray: IntArray,
        product: IntArray, productOff: Int,
        multiplicand: IntArray, multiplicandOff: Int, multiplicandLen: Int,
        multiplierDw2: Int, multiplierDw1: Int, multiplierDw0: Int) {

        var dw2dw1back3 = 0
        var dw1dw1back2 = 0
        var(dw2dw1back2, dw2dw0back2) = 0 to 0
        var dw0dw1back1 = 0
        var (dw1dw1back1, dw1dw0back1) = 0 to 0
        var(dw2dw1back1, dw2dw0back1) = 0 to 0
        var carryIn = 0


        var i = 0
        while (i < multiplicandLen + 3) {
            val m = if (i < multiplicandLen) multiplicand[multiplicandOff + i] else 0

            val (dw0dw1, dw0dw0) = multu32u32u64(m, multiplierDw0)

            val (carry, currentDigit) = sumU32(dw2dw1back3, dw1dw1back2, dw2dw0back2, dw0dw1back1, dw1dw0back1, dw0dw0, carryIn)
            product[productOff + i] = currentDigit
            val (dw1dw1, dw1dw0) = multu32u32u64(m, multiplierDw1)
            val (dw2dw1, dw2dw0) = multu32u32u64(m, multiplierDw2)

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

    fun arrayFrom(str: String) = arrayFrom(BigInteger(str))

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

    data class TC(val multiplicand: IntArray, val multiplierDw2: Int, val multiplierDw1: Int, val multiplierDw0: Int)

    val tcs = arrayOf(
        TC(intArrayOf(-3, -2, -1), 0, 1 shl 31, -1),
        TC(intArrayOf(-1, 1 shl 31), -1, -2, -3),

        TC(intArrayOf(-1, -1, -1, -1), -1, -1, -1),

        TC(intArrayOf(0, -2), 0, -4, -5),
        TC(intArrayOf(-1, -1), 0, -1, 0),
        TC(intArrayOf(-1, -1), 0, 0, -1),
        TC(intArrayOf(-1, -1), 0, -1, 0),
        TC(intArrayOf(-1, -1), 0, 0, 1),
        TC(intArrayOf(-1, -1), 0, 1, 0),
        TC(intArrayOf(1), 0, 0, 1),
        TC(intArrayOf(1), 0,1, 0),
        TC(intArrayOf(-1), 0, 0, 1),
        TC(intArrayOf(-1), 0, 1, 0),
        TC(intArrayOf(1), 0, 0, -1),
        TC(intArrayOf(1), 0,-1, 0),
        TC(intArrayOf(-1), 0, 0, -1),
        TC(intArrayOf(-1), 0, -1, 0),
        TC(intArrayOf(1), 0, 0, 1),
        TC(intArrayOf(1), 0, 1, 0),
        TC(intArrayOf(1), 0, 0, 1),
        TC(intArrayOf(1), 0, 1, 0),
        TC(intArrayOf(2), 0, 3, 4),
        TC(intArrayOf(0, 1), 0, 1, 0),
        TC(intArrayOf(1, 2, 3), 0, 4, 5),
        TC(intArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), 0, 2, (1 shl 31)),
        TC(intArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), 0, 1, (1 shl 31)),

        TC(intArrayOf((1 shl 31), (1 shl 31), (1 shl 31), (1 shl 31)), 0, (1 shl 31), (1 shl 31)),

        TC(intArrayOf(-1, -1), -1, 0, 0),

        TC(intArrayOf(-1, -1), -1, 0, 0),
        TC(intArrayOf(-1, -1), 1, 0, 0),

        TC(intArrayOf(-1), -1, 0, 0),

        TC(intArrayOf(1), 1, 0, 0),
        TC(intArrayOf(-1), 1, 0, 0),
        TC(intArrayOf(1), -1, 0, 0),
        TC(intArrayOf(-1), -1, 0, 0),

        TC(intArrayOf(1), 1, 0, 0),
        TC(intArrayOf(1), 4, 2, 0),
        TC(intArrayOf((1 shl 63)), 1, 0, 1 shl 63),
        TC(intArrayOf(1), 1 shl 31, 0, 0),
        TC(intArrayOf(-1), -1, -1, -1),

        TC(intArrayOf(-1, -1, -1, -1), -1, -1, -1),


    )

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
        val product = IntArray(tc.multiplicand.size + 3)
        multiplyX3(expectedAsArray, product, 0, tc.multiplicand, 0, tc.multiplicand.size, tc.multiplierDw2, tc.multiplierDw1, tc.multiplierDw0)
        val biProduct = biFromArray(product)
        //if (! expected.equals(biProduct))
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
        val multiplicandOff = 0 //random.nextInt(3)
        val multiplicand = IntArray(multiplicandOff + multiplicandLen)
        for (i in 0..<multiplicandLen)
            multiplicand[multiplicandOff + i] = biMultiplicand.shiftRight(i * 32).toInt()

        val multiplierBitLength = random.nextInt(33, 97)
        val biMultiplier = BigInteger(multiplierBitLength, random)
        val multiplierW2 = biMultiplier.shiftRight(64).toInt()
        val multiplierW1 = biMultiplier.shiftRight(32).toInt()
        val multiplierW0 = biMultiplier.toInt()

        val prodOff = 0 //random.nextInt(3)
        val prod = IntArray(prodOff + multiplicandLen + 3)
        val expected = biMultiplicand.multiply(biMultiplier)
        val expectedAsArray = arrayFrom(expected)

        multiplyX3(expectedAsArray, prod, prodOff, multiplicand, multiplicandOff, multiplicandLen,
            multiplierW2, multiplierW1, multiplierW0)

        val biProd = biFromArray(prod, prodOff, multiplicandLen + 3)

        if (! expected.equals(biProd)) {
            println("expected:$expected observed:$biProd multiplicand:$biMultiplicand multiplier $biMultiplier")
            println("expectedAsArray:" + expectedAsArray.joinToString(prefix = "[", postfix = "]", separator = ", ") {
                "0x" + it.toString(16).uppercase()})
            println("prodAsArray:" + prod.joinToString(prefix = "[", postfix = "]", separator = ", ") {
                "0x" + it.toString(16).uppercase()})
        }

        assert(expected.equals(biProd))
    }
}