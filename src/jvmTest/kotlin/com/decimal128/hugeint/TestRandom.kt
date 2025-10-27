package com.decimal128.hugeint

import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigInteger
import kotlin.random.Random
import kotlin.test.Test

class TestRandom {

    @Test
    fun test1() {
        for (i in 0..<1000000) {
            val hi = randomHi(1024)
            val bi = BigInteger(hi.toBigEndianTwosComplementByteArray())

            val hiBitLen = hi.magnitudeBitLen()
            val biBitLen = bi.bitLength()
            assertEquals(biBitLen, hiBitLen)

            val hiBitCount = hi.magnitudeBitCount()
            val biBitCount = bi.bitCount()
            assertEquals(biBitCount, hiBitCount)

            val hiNtz = hi.getLowestSetBit()
            val biNtz = bi.getLowestSetBit()
            assertEquals(biNtz, hiNtz)
        }
    }

    val rng = Random.Default

    fun randomHi(hiBitLen: Int) =
        HugeInt.fromRandom(rng.nextInt(hiBitLen), rng)

}