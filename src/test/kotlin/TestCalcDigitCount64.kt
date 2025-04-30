package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

/*
fun getDigitCount(n: Int) : Int {
    val digitCount = 1 +
            ((9 - n) ushr 31) + ((99 - n) ushr 31) + ((999 - n) ushr 31) +
            ((9999 - n) ushr 31) + ((99999 - n) ushr 31) + ((999999 - n) ushr 31) +
            ((9999999 - n) ushr 31) + ((99999999 - n) ushr 31) + ((999999999 - n) ushr 31)
    return digitCount
}
*/
@OptIn(ExperimentalUnsignedTypes::class)
class TestCalcDigitCount64 {

    @Test
    fun test() {
        test1(Long.MAX_VALUE.toULong())
        test1(Long.MIN_VALUE.toULong())
        test1(ULong.MAX_VALUE)
        //var ul = 10_000_000_000_000_000_000uL
        var ul = 1uL
        while (true) {
            test3(ul)
            if (ul == 10_000_000_000_000_000_000uL)
                break
            ul *= 10uL
        }
    }

    fun test3(ul: ULong) {
        test1(ul - 1uL)
        test1(ul)
        test1(ul + 1uL)
    }

    fun test1(ul: ULong) {
        val ulStrLen = ul.toString().length
        val expected = if (ul == 0uL) 0 else ulStrLen
        val observed = CoeffDigitCount.calcDigitCount64(ul)
        if (expected != observed)
            println("expected:$expected  observed:$observed  ul:$ul")
        assertEquals(expected, observed)
    }

    @Test
    fun testRandom() {
        for (i in 1..100000)
            test1Random()
    }

    val random = Random()

    fun test1Random() {
        var bitLength = 0
        do {
            bitLength = random.nextInt(0, 65)
        } while (bitLength == 0)
        val l = random.nextLong()
        val m = if (bitLength == 64) l else ((1L shl bitLength) - 1)
        test1(m.toULong())
    }



}