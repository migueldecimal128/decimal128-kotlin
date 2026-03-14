package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.lang.Long.bitCount
import java.lang.Math.unsignedMultiplyHigh
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.util.*
import java.lang.Math.min
import kotlin.test.assertEquals

class TestReduce {

    val verbose = false

    val mask0 = 0x1111111111111111L
    val mask1 = mask0 shl 1
    val mask2 = mask0 shl 2
    val mask3 = mask0 shl 3

    fun isMultipleOfFive_64(dw0: Long): Boolean {
        val count0 = bitCount(dw0 and mask0)
        val count1 = bitCount(dw0 and mask1)
        val count2 = bitCount(dw0 and mask2)
        val count3 = bitCount(dw0 and mask3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    fun isMultipleOfFive_128(dw1:Long, dw0: Long): Boolean {
        val count0 = bitCount(dw1 and mask0) + bitCount(dw0 and mask0)
        val count1 = bitCount(dw1 and mask1) + bitCount(dw0 and mask1)
        val count2 = bitCount(dw1 and mask2) + bitCount(dw0 and mask2)
        val count3 = bitCount(dw1 and mask3) + bitCount(dw0 and mask3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    fun isMultipleOfFive_192(dw2: Long, dw1:Long, dw0: Long): Boolean {
        val count0 = bitCount(dw2 and mask0) + bitCount(dw1 and mask0) + bitCount(dw0 and mask0)
        val count1 = bitCount(dw2 and mask1) + bitCount(dw1 and mask1) + bitCount(dw0 and mask1)
        val count2 = bitCount(dw2 and mask2) + bitCount(dw1 and mask2) + bitCount(dw0 and mask2)
        val count3 = bitCount(dw2 and mask3) + bitCount(dw1 and mask3) + bitCount(dw0 and mask3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    fun isMultipleOfFive_256(dw3:Long, dw2: Long, dw1:Long, dw0: Long): Boolean {
        val count0 = bitCount(dw3 and mask0) + bitCount(dw2 and mask0) +
                bitCount(dw1 and mask0) + bitCount(dw0 and mask0)
        val count1 = bitCount(dw3 and mask1) + bitCount(dw2 and mask1) +
                bitCount(dw1 and mask1) + bitCount(dw0 and mask1)
        val count2 = bitCount(dw3 and mask2) + bitCount(dw2 and mask2) +
                bitCount(dw1 and mask2) + bitCount(dw0 and mask2)
        val count3 = bitCount(dw3 and mask3) + bitCount(dw2 and mask3) +
                bitCount(dw1 and mask3) + bitCount(dw0 and mask3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    fun calcReductionPow5_64(dw: Long, pow2Limit: Int): Int {
        assert(dw != 0L)
        var k = 0
        var d = dw
        while (k < pow2Limit) {
            val m = 0xCCCC_CCCC_CCCC_CCCDuL.toLong()
            val hi = unsignedMultiplyHigh(d, m)
            val div5 = hi ushr 2
            if ((div5 * 5) != d)
                return k
            ++k
            d = div5
        }
        return k
    }

    val coeffReduceDivTmp = C256()

    fun calcReductionPow10(x: C256): Int {
        val ntz = x.c256NumberTrailingZeros()
        if (ntz == 0)
            return 0
        if ((x.bitLen - ntz) <= 64) {
            val dw = x.c256DwordAtBitIndex(ntz)
            return calcReductionPow5_64(dw, ntz)
        }
        var ntzRemaining = ntz
        coeffReduceDivTmp.c256SetShiftRight(x, ntz)
        var r = 0L // remainder
        do {
            val powStep = min(ntzRemaining, BARRETT_POW5_MU_MAXX - 1)
            r = barrettDivModPow5(coeffReduceDivTmp, coeffReduceDivTmp, powStep)
            if (r != 0L) {
                val rReduction = calcReductionPow5_64(r, ntzRemaining)
                ntzRemaining -= rReduction
                break;
            }
            ntzRemaining -= powStep
        } while (ntzRemaining > 0)
        val k = ntz - ntzRemaining
        return k
    }

    fun reduce(z: C256, x: C256): Int {
        if (x.bitLen > 0 && isMultipleOfFive_256(x.dw3, x.dw2, x.dw1, x.dw0)) {
            val k = calcReductionPow10(x)
            if (k > 0) {
                val residue = c256SetScaleDownPow10(z, x, k, Pentad())
                assert(residue == Residue.EXACT)
                return k
            }
        }
        z.c256Set(x)
        return 0
    }

    val FIVE = BigInteger.valueOf(5L)

    class TC(val bi: BigInteger) {
        constructor(str: String) : this(BigInteger(str))
        constructor(l: Long) : this(BigInteger.valueOf(l))
        val bitLen = run {val bitLen = bi.bitLength(); require(bitLen <= 256); bitLen}
        val reductionPow10 = run {
            if (bitLen == 0)
                0
            else {
                var k = 0
                var biT = bi
                while (biT.mod(BigInteger.TEN).equals(ZERO)) {
                    biT = biT.divide(BigInteger.TEN)
                    ++k
                }
                k
            }
        }
    }

    val tcs = arrayOf(
        TC("123456789123456789123456789123456789123456789"),
        TC(FIVE.pow(110)),
        TC("12345678901234567890123456789123456789123456789123456789123456891"),
        TC("12345678989012345600"),
        TC("14600000000000000000"), // 14600000000000000000
        TC("5000000000000000"),
        TC("582239057842592488800"),
        TC("572135574136166865449429225266381990144000000000000"),
        TC("4000000000000"),
        TC(15),
        TC("42288349021474833586838254686343304246565023760693060"),
        TC(10),
        TC(1),
        TC(4),
        TC(5),
        TC(6),
        TC(25),
        TC(625),
        TC(FIVE.pow(110)),
        TC(FIVE.pow(110).add(ONE)),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            var bi = ZERO
            do {
                var pow10: Int
                do {
                    pow10 = min(random.nextInt(30), random.nextInt(30))
                } while (pow10 == 0)
                bi = randBi().multiply(BigInteger.TEN.pow(pow10))
            } while (bi.bitLength() > 256)
            val case = TC(bi)
            test1(case)
        }

    }

    val random = Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 256)
        val bi = BigInteger(bitLength, random)
        return bi
    }


    fun test1(tc: TC) {
        val bi = tc.bi
        val bitLen = tc.bitLen
        val expected = tc.reductionPow10
        if (verbose)
            println("$bi ($bitLen) reductionPow10:$expected")
        val c = newCoeff(bi)
        val z = C256()
        val observed = reduce(z, c)
        assertEquals(expected, observed)
    }

}