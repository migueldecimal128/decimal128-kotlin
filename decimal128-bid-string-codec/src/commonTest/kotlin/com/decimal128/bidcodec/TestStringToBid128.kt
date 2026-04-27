package com.decimal128.bidcodec

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TestStringToBid128 {

    val verbose = true

    data class TC(val expectedHex: String, val str: String)

    val tcs = arrayOf(
        TC("[7E00000000000001,0000000000000000]", "sNaN18446744073709551616"),

        // === Specials: Infinity ===
        TC("[7800000000000000,0000000000000000]", "Infinity"),       // +Inf, canonical
        TC("[F800000000000000,0000000000000000]", "-Infinity"),      // -Inf

        // NaNs
        TC("[7C00000000000000,0000000000000000]", "NaN"),            // quiet NaN, no payload
        TC("[FC00000000000000,0000000000000000]", "-NaN"),           // negative quiet NaN (sign bit set)
        TC("[7C00000000000000,0000000000000001]", "NaN1"),           // payload = 1
        TC("[7E00000000000000,0000000000000000]", "sNaN"),           // signaling NaN
        TC("[7E00000000000000,0000000000000001]", "sNaN1"),
        TC("[FE00000000000000,0000000000000000]", "-sNaN"),           // signaling NaN
        TC("[FE00000000000000,0000000000000001]", "-sNaN1"),
        TC("[7E00000000000000,7FFFFFFFFFFFFFFF]", "sNaN9223372036854775807"),
        TC("[7E00000000000000,8000000000000000]", "sNaN9223372036854775808"),
        TC("[7E00000000000000,FFFFFFFFFFFFFFFF]", "sNaN18446744073709551615"),
        TC("[7E00000000000001,0000000000000000]", "sNaN18446744073709551616"),
        TC("[7C00314DC6448D93,38C15B09FFFFFFFF]", "NaN999999999999999999999999999999999"), // max payload
        // too many digits! return payload == 0
        TC("[7C00000000000000,0000000000000000]", "NaN9999999999999999999999999999999999"),

        /*
        TC("[0000000000000000,0000000000000000]", "0E-6176"),       // smallest exponent zero
        TC("[3040000000000000,0000000000000000]", "0"),              // exponent 0 zero (canonical "0")
        TC("[5FFE000000000000,0000000000000000]", "0E6111"),        // largest exponent zero
        TC("[B040000000000000,0000000000000000]", "-0"),             // negative zero, exponent 0

         */
        )

    @Test
    fun testCases() {
        for (tc in tcs)
            test(tc)
    }

    fun test(tc: TC) {
        if (verbose)
            println(tc)
        val longs = longArrayOf(0xBEEF, 0xCAFE)
        val observed = Decimal128BidStringCodec.parseReturnError(longs, tc.str)
        if (tc.expectedHex == "invalid") {
            assertNotNull(observed)
            assertEquals(0xBEEF, longs[0])
            assertEquals(0xCAFE, longs[1])
            return
        }
        assertNull(observed)
        val observedHex = "[${hex16(longs[0])},${hex16(longs[1])}]"
        assertEquals(tc.expectedHex, observedHex)
    }

    private fun hex16(v: Long): String =
        v.toULong().toString(16).padStart(16, '0').uppercase()

    private fun bid128ParseIntelHex(str: String): Pair<Long, Long> {
        if (str.length !in 34..35 ||
            str[0] != '[' || str[str.lastIndex] != ']' ||
            str.length == 35 && str[17] != ','
        ) {
            throw IllegalArgumentException("Invalid Intel BID Hex string - overall")
        }
        val (bid128Hi, isValidHi) = parseHexDword(str, 1)
        if (! isValidHi)
            throw IllegalArgumentException("Invalid Intel BID Hex string - first dword")
        val (bid128Lo, isValidLo) = parseHexDword(str, (str.length + 1) shr 1)
        if (! isValidLo)
            throw IllegalArgumentException("Invalid Intel BID Hex string - second dword")
        return bid128Hi to bid128Lo
    }

    private fun parseHexDword(str: String, off: Int): Pair<Long, Boolean> {
        var dw = 0L
        for (i in 0..15) {
            val ch = str[off + i]
            dw = when (ch) {
                in '0'..'9' -> (dw shl 4) or (ch - '0').toLong()
                in 'A'..'F' -> (dw shl 4) or (ch - 'A' + 10).toLong()
                in 'a'..'f' -> (dw shl 4) or (ch - 'a' + 10).toLong()
                else -> {
                    return 0L to false
                }
            }
        }
        return dw to true
    }


}