package com.decimal128

import com.decimal128.TestCoeffMul.TC
import org.junit.jupiter.api.Test
import java.lang.Long.bitCount
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.util.*
import kotlin.test.assertEquals

const val mask0 = 0x1111111111111111L
const val mask1 = mask0 shl 1
const val mask2 = mask0 shl 2
const val mask3 = mask0 shl 3

class TestisMultipleOfFive {

    val verbose = true

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

    val FIVE = BigInteger.valueOf(5L)

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