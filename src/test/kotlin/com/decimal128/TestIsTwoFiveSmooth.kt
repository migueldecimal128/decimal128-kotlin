package com.decimal128

import org.junit.jupiter.api.Test
import java.lang.Long.numberOfLeadingZeros
import java.lang.Long.numberOfTrailingZeros
import java.math.BigInteger
import java.math.BigInteger.ONE
import kotlin.test.assertEquals

val FIVE = BigInteger.valueOf(5L)

class TestIsTwoFiveSmooth {

    val verbose = true

    val maxPow5 = 111
    val POWERS5 = LongArray(4 * maxPow5)
    val BIT_LEN_TO_POW5 = ByteArray(257)

    fun initialize() {
        var pow5Prev = ONE
        for (p5 in 1..<maxPow5) {
            val pow5 = pow5Prev.multiply(FIVE)
            val bitLen = pow5.bitLength()
            BIT_LEN_TO_POW5[bitLen] = p5.toByte()
            if (verbose)
                println("5**$p5 bitLen:$bitLen")
            val offset = 4 * p5
            POWERS5[offset + 0] = pow5.toLong()
            POWERS5[offset + 1] = pow5.shiftRight(64).toLong()
            POWERS5[offset + 2] = pow5.shiftRight(128).toLong()
            POWERS5[offset + 3] = pow5.shiftRight(192).toLong()
            pow5Prev = pow5
        }
    }

    fun isPow5_64(bitLen: Int, dw0: Long): Boolean {
        assert((dw0 and 1L) != 0L)
        assert(bitLen == 64 - java.lang.Long.numberOfLeadingZeros(dw0))
        val p5 = BIT_LEN_TO_POW5[bitLen].toInt()
        val offset = 4 * p5
        return (p5 != 0) && POWERS5[offset + 0] == dw0
    }

    fun isPow5_128(bitLen: Int, dw1: Long, dw0: Long): Boolean {
        assert((dw0 and 1L) != 0L)
        assert(dw1 != 0L)
        assert(bitLen == 128 - java.lang.Long.numberOfLeadingZeros(dw1))
        val p5 = BIT_LEN_TO_POW5[bitLen].toInt()
        val offset = 4 * p5
        return (p5 != 0) && POWERS5[offset + 0] == dw0 && POWERS5[offset + 1] == dw1
    }

    fun isPow5_192(bitLen: Int, dw2: Long, dw1: Long, dw0: Long): Boolean {
        assert((dw0 and 1L) != 0L)
        assert(dw2 != 0L)
        assert(bitLen == 192 - java.lang.Long.numberOfLeadingZeros(dw2))
        val p5 = BIT_LEN_TO_POW5[bitLen].toInt()
        val offset = 4 * p5
        return (p5 != 0) &&
                POWERS5[offset + 0] == dw0 && POWERS5[offset + 1] == dw1 &&
                POWERS5[offset + 2] == dw2

    }

    fun isPow5_256(bitLen: Int, dw3:Long, dw2: Long, dw1: Long, dw0: Long): Boolean {
        assert((dw0 and 1L) != 0L)
        assert(dw3 != 0L)
        assert(bitLen == 256 - java.lang.Long.numberOfLeadingZeros(dw3))
        val p5 = BIT_LEN_TO_POW5[bitLen].toInt()
        val offset = 4 * p5
        return (p5 != 0) &&
                POWERS5[offset + 0] == dw0 && POWERS5[offset + 1] == dw1 &&
                POWERS5[offset + 2] == dw2 && POWERS5[offset + 3] == dw3

    }

    fun isTwoFiveSmooth_64(dw0: Long): Boolean {
        val ntz = numberOfTrailingZeros(dw0)
        val oddPart0 = (dw0 ushr ntz)
        val bitLen = 64 - numberOfLeadingZeros(oddPart0)
        if (bitLen <= 1)
            return bitLen == 1
        val p5 = BIT_LEN_TO_POW5[bitLen].toInt()
        val offset = 4 * p5
        return (p5 != 0) && POWERS5[offset + 0] == oddPart0
    }

    fun isTwoFiveSmooth_128(dw1:Long, dw0: Long): Boolean {
        if (dw0 == 0L)
            return isTwoFiveSmooth_64(dw1)
        assert(dw1 != 0L)
        val ntz = numberOfTrailingZeros(dw0)
        val ntzNonZeroMask = (-ntz shr 31).toLong()
        val bitLen = 128 - numberOfLeadingZeros(dw1)
        val oddPart0 = (ntzNonZeroMask and (dw1 shl -ntz)) or (dw0 ushr ntz)
        val oddPart1 =                                        (dw1 ushr ntz)
        val bitLenOdd = bitLen - ntz
        val p5 = BIT_LEN_TO_POW5[bitLenOdd].toInt()
        val offset = 4 * p5
        return (p5 != 0) &&
                POWERS5[offset + 0] == oddPart0 && POWERS5[offset + 1] == oddPart1
    }

    fun isTwoFiveSmooth_192(dw2: Long, dw1:Long, dw0: Long): Boolean {
        if (dw0 == 0L)
            return isTwoFiveSmooth_128(dw2, dw1)
        assert(dw2 != 0L)
        val ntz = numberOfTrailingZeros(dw0)
        val ntzNonZeroMask = (-ntz shr 31).toLong()
        val bitLen = 192 - numberOfLeadingZeros(dw2)
        val oddPart0 = (ntzNonZeroMask and (dw1 shl -ntz)) or (dw0 ushr ntz)
        val oddPart1 = (ntzNonZeroMask and (dw2 shl -ntz)) or (dw1 ushr ntz)
        val oddPart2 =                                        (dw2 ushr ntz)
        val bitLenOdd = bitLen - ntz
        val p5 = BIT_LEN_TO_POW5[bitLenOdd].toInt()
        val offset = 4 * p5
        return (p5 != 0) &&
                POWERS5[offset + 0] == oddPart0 && POWERS5[offset + 1] == oddPart1 &&
                POWERS5[offset + 2] == oddPart2
    }

    fun isTwoFiveSmooth_256(dw3:Long, dw2: Long, dw1:Long, dw0: Long): Boolean {
        if (dw0 == 0L)
            return isTwoFiveSmooth_192(dw3, dw2, dw1)
        assert(dw3 != 0L)
        val ntz = numberOfTrailingZeros(dw0)
        val ntzNonZeroMask = (-ntz shr 31).toLong()
        val bitLen = 256 - numberOfLeadingZeros(dw2)
        val oddPart0 = (ntzNonZeroMask and (dw1 shl -ntz)) or (dw0 ushr ntz)
        val oddPart1 = (ntzNonZeroMask and (dw2 shl -ntz)) or (dw1 ushr ntz)
        val oddPart2 = (ntzNonZeroMask and (dw3 shl -ntz)) or (dw2 ushr ntz)
        val oddPart3 =                                        (dw3 ushr ntz)
        val bitLenOdd = bitLen - ntz
        val p5 = BIT_LEN_TO_POW5[bitLenOdd].toInt()
        val offset = 4 * p5
        return (p5 != 0) &&
                POWERS5[offset + 0] == oddPart0 && POWERS5[offset + 1] == oddPart1 &&
                POWERS5[offset + 2] == oddPart2 && POWERS5[offset + 3] == oddPart3
    }

    class TC(val bi: BigInteger) {
        constructor(str: String) : this(BigInteger(str))
        constructor(l: Long) : this(BigInteger.valueOf(l))
        val bitLen = run {val bitLen = bi.bitLength(); require(bitLen <= 256); bitLen}
        val isTwoFiveSmooth = run {
            if (bi.compareTo(BigInteger.ONE) < 0)
                false
            else {
                var biT = bi.shiftRight(bi.lowestSetBit)
                while (biT.mod(FIVE).equals(BigInteger.ZERO))
                    biT = biT.divide(FIVE);
                biT.equals(BigInteger.ONE);
            }
        }
    }

    val tcs = arrayOf(
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
        initialize()
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        val bi = tc.bi
        val bitLen = tc.bitLen
        val expected = tc.isTwoFiveSmooth
        if (verbose)
            println("$bi ($bitLen) isTwoFiveSmooth:$expected")
        val dw0 = bi.toLong()
        val dw1 = bi.shiftRight(64).toLong()
        val dw2 = bi.shiftRight(128).toLong()
        val dw3 = bi.shiftRight(192).toLong();
        val observed = when {
            bitLen <= 64 ->
                isTwoFiveSmooth_64(dw0)
            bitLen <= 128 ->
                isTwoFiveSmooth_128(dw1, dw0)
            bitLen <= 192 ->
                isTwoFiveSmooth_192(dw2, dw1, dw0)
            bitLen <= 256 ->
                isTwoFiveSmooth_256(dw3, dw2, dw1, dw0)
            else ->
                throw RuntimeException("?que?")
        }
        assertEquals(expected, observed)
    }

}