package com.decimal128

import org.junit.jupiter.api.Test
import java.lang.Long.bitCount
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.util.*
import kotlin.test.assertEquals

class TestIsMultipleOfFive {

    val verbose = false

    private val MASK_BITS_0_MOD_4 = 0x1111111111111111L
    private val MASK_BITS_1_MOD_4 = MASK_BITS_0_MOD_4 shl 1
    private val MASK_BITS_2_MOD_4 = MASK_BITS_0_MOD_4 shl 2
    private val MASK_BITS_3_MOD_4 = MASK_BITS_0_MOD_4 shl 3

    fun isMultipleOfFive_64(dw0: Long): Boolean {
        val m0 = MASK_BITS_0_MOD_4
        val m1 = MASK_BITS_1_MOD_4
        val m2 = MASK_BITS_2_MOD_4
        val m3 = MASK_BITS_3_MOD_4

        val count0 = bitCount(dw0 and m0)
        val count1 = bitCount(dw0 and m1)
        val count2 = bitCount(dw0 and m2)
        val count3 = bitCount(dw0 and m3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    fun isMultipleOfFive_128(dw1:Long, dw0: Long): Boolean {
        val m0 = MASK_BITS_0_MOD_4
        val m1 = MASK_BITS_1_MOD_4
        val m2 = MASK_BITS_2_MOD_4
        val m3 = MASK_BITS_3_MOD_4

        val count0 = bitCount(dw1 and m0) + bitCount(dw0 and m0)
        val count1 = bitCount(dw1 and m1) + bitCount(dw0 and m1)
        val count2 = bitCount(dw1 and m2) + bitCount(dw0 and m2)
        val count3 = bitCount(dw1 and m3) + bitCount(dw0 and m3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    fun isMultipleOfFive_192(dw2: Long, dw1:Long, dw0: Long): Boolean {
        val m0 = MASK_BITS_0_MOD_4
        val m1 = MASK_BITS_1_MOD_4
        val m2 = MASK_BITS_2_MOD_4
        val m3 = MASK_BITS_3_MOD_4

        val count0 = bitCount(dw2 and m0) + bitCount(dw1 and m0) + bitCount(dw0 and m0)
        val count1 = bitCount(dw2 and m1) + bitCount(dw1 and m1) + bitCount(dw0 and m1)
        val count2 = bitCount(dw2 and m2) + bitCount(dw1 and m2) + bitCount(dw0 and m2)
        val count3 = bitCount(dw2 and m3) + bitCount(dw1 and m3) + bitCount(dw0 and m3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    fun isMultipleOfFive_256(dw3:Long, dw2: Long, dw1:Long, dw0: Long): Boolean {
        val m0 = MASK_BITS_0_MOD_4
        val m1 = MASK_BITS_1_MOD_4
        val m2 = MASK_BITS_2_MOD_4
        val m3 = MASK_BITS_3_MOD_4

        val count0 = bitCount(dw3 and m0) + bitCount(dw2 and m0) +
                bitCount(dw1 and m0) + bitCount(dw0 and m0)
        val count1 = bitCount(dw3 and m1) + bitCount(dw2 and m1) +
                bitCount(dw1 and m1) + bitCount(dw0 and m1)
        val count2 = bitCount(dw3 and m2) + bitCount(dw2 and m2) +
                bitCount(dw1 and m2) + bitCount(dw0 and m2)
        val count3 = bitCount(dw3 and m3) + bitCount(dw2 and m3) +
                bitCount(dw1 and m3) + bitCount(dw0 and m3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    class TC(val bi: BigInteger) {
        constructor(str: String) : this(BigInteger(str))
        constructor(l: Long) : this(BigInteger.valueOf(l))
        val bitLen = run {val bitLen = bi.bitLength(); require(bitLen <= 256); bitLen}
        val isMultipleOfFive = bi.divide(FIVE).multiply(FIVE) == bi
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
            val case = TC(randBi())
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
        val expected = tc.isMultipleOfFive
        if (verbose)
            println("$bi ($bitLen) isMultipleOfFive:$expected")
        val dw0 = bi.toLong()
        val dw1 = bi.shiftRight(64).toLong()
        val dw2 = bi.shiftRight(128).toLong()
        val dw3 = bi.shiftRight(192).toLong();
        val observed = when {
            bitLen <= 64 ->
                isMultipleOfFive_64(dw0)
            bitLen <= 128 ->
                isMultipleOfFive_128(dw1, dw0)
            bitLen <= 192 ->
                isMultipleOfFive_192(dw2, dw1, dw0)
            bitLen <= 256 ->
                isMultipleOfFive_256(dw3, dw2, dw1, dw0)
            else ->
                throw RuntimeException("?que?")
        }
        assertEquals(expected, observed)
    }

}