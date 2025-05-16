package com.decimal128

import org.junit.jupiter.api.Test
import java.lang.Long.bitCount
import java.lang.Long.numberOfTrailingZeros
import java.lang.Math.unsignedMultiplyHigh
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.util.*
import kotlin.math.min
import kotlin.test.assertEquals

class TestReduce {

    val verbose = true

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

    fun calcReduction_64(dw: Long): Int {
        assert(dw != 0L)
        var k = 0
        while (true) {
            val div5 = unsignedMultiplyHigh(dw, 0xCCCC_CCCC_CCCC_CCCDuL.toLong()) ushr 3
            if ((div5 * 5) != dw)
                return k
            ++k
        }
    }

    val coeffReduceDivT = Coeff()
    val coeffReduceModT = Coeff()

    fun calcReduction(x: Coeff): Int {
        val ntz = x.coeffNumberOfTrailingZeros()
        if ((x.bitLen - ntz) <= 64) {
            val dw = x.coeffDwordAtBitIndex(ntz)
            return calcReduction_64(dw)
        }
        var ntzRemaining = ntz
        var thisStride = min(ntzRemaining, 18)
        //FIXME ... to be continued ...
        return -1
    }

    fun reduce(z: Coeff, x: Coeff): Int {
        if (x.bitLen > 0 && isMultipleOfFive_256(x.dw3, x.dw2, x.dw1, x.dw0)) {
            val k = calcReduction(x)
            if (k > 0) {
                val residue = z.scaleDownPow10(x, k)
                assert(residue == Residue.EXACT)
                return k
            }
        }
        z.coeffSet(x)
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
        TC("42288349021474833586838254686343304246565023760693060"),
        TC(10),
        TC(15),
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
                val pow10 = min(random.nextInt(30), random.nextInt(30))
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
        val c = Coeff(bi)
        val z = Coeff()
        val observed = reduce(z, c)
        assertEquals(expected, observed)
    }

}