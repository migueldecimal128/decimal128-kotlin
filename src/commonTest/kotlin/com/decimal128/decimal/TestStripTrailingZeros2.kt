package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

private const val M_U32_DIV_1E1 = 0xCCCCCCCDL
private const val S_U32_DIV_1E1 = 35

private const val M_U32_DIV_1E2 = 0x51EB851FL
private const val S_U32_DIV_1E2 = 37

private const val M_U32_DIV_1E4 = 0x346DC5D7L
private const val S_U32_DIV_1E4 = 43

class TestStripTrailingZeros2 {

    @Test
    fun testCtzd() {
        assertEquals(0, ctzd32(99999))
        assertEquals(1, ctzd32(987650))
        assertEquals(2, ctzd32(100))
        assertEquals(3, ctzd32(99000))
        assertEquals(4, ctzd32(44440000))
        assertEquals(5, ctzd32(500000))
        assertEquals(6, ctzd32(6000000))
        assertEquals(7, ctzd32(70000000))
        assertEquals(8, ctzd32(800000000))
        assertEquals(9, ctzd32(2000000000))

    }

    fun ctzd32(n: Int): Int {
        var r = n
        var ntzd = 0

        if (r >= 1_0000_0000 && r % 1_0000_0000 == 0) {
            r /= 1_0000_0000
            ntzd += 8
        }
        if (r >= 1_0000 && r % 1_0000 == 0) {
            r /= 1_0000
            ntzd += 4
        }
        if (r >= 100 && r % 100 == 0) {
            r /= 100
            ntzd += 2
        }
        if (r >= 10 && r % 10 == 0) {
            r /= 10
            ntzd += 1
        }

        return ntzd
    }

    @Test
    fun testCtzd_recipMul() {
        assertEquals(8, ctzd32_recipMul(800000000))
        assertEquals(0, ctzd32_recipMul(99999))
        assertEquals(1, ctzd32_recipMul(987650))
        assertEquals(2, ctzd32_recipMul(100))
        assertEquals(3, ctzd32_recipMul(99000))
        assertEquals(4, ctzd32_recipMul(44440000))
        assertEquals(5, ctzd32_recipMul(500000))
        assertEquals(6, ctzd32_recipMul(6000000))
        assertEquals(7, ctzd32_recipMul(70000000))
        assertEquals(8, ctzd32_recipMul(800000000))
        assertEquals(9, ctzd32_recipMul(1000000000))

    }

    fun ctzd32_recipMul(n: Int): Int {
        check(n > 0 && n <= 1_000_000_000)
        var d = n.toLong()
        var ntzd = 0

        var q: Long
        var r: Long
        var mask: Long

        q = (d * M_U32_DIV_1E4) ushr S_U32_DIV_1E4
        r = d - (q * 1_0000)
        mask = ((d - 10000) or (-r)) shr 63
        d = (d and mask) or (q and mask.inv())
        ntzd += 4 and (mask.inv()).toInt()

        q = (d * M_U32_DIV_1E4) ushr S_U32_DIV_1E4
        r = d - (q * 1_0000)
        mask = ((d - 10000) or (-r)) shr 63
        d = (d and mask) or (q and mask.inv())
        ntzd += 4 and (mask.inv()).toInt()

        q = (d * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        r = d - (q * 100)
        mask = ((d - 100) or (-r)) shr 63
        d = (d and mask) or (q and mask.inv())
        ntzd += 2 and (mask.inv()).toInt()

        q = (d * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        r = d - (q * 10)
        mask = ((d - 10) or (-r)) shr 63
        ntzd += 1 and (mask.inv()).toInt()

        return ntzd
    }

}
