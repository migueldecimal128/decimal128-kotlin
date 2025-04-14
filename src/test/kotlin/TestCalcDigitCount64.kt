package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
        //var ul = 10_000_000_000_000_000_000uL
        var ul = 1uL
        while (true) {
            test3(ul)
            if (ul == 10_000_000_000_000_000_000uL)
                break;
            ul *= 10uL
        }
        test1(ULong.MAX_VALUE)
    }

    fun test3(ul: ULong) {
        test1(ul - 1uL)
        test1(ul)
        test1(ul + 1uL)
    }

    fun test1(ul: ULong) {
        val ulStrLen = ul.toString().length
        val expected = if (ul == 0uL) 0 else ulStrLen
        val observed = calcDigitCount64(ul)
        if (expected != observed)
            println("expected:$expected  observed:$observed  ul:$ul")
        assertEquals(expected, observed)
    }

}